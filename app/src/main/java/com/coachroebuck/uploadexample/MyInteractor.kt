package com.coachroebuck.uploadexample

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.os.Handler
import android.util.Log
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.net.Socket

class MyInteractor(private val adapter: MyAdapter = MyAdapter()) {

    private var createWebSocketCall: Call<JsonElement>? = null

    private val tag = this::class.java.canonicalName

    fun upload(cr: ContentResolver?, data: Intent?) {
        val successCallback = { portId: Long ->
            openWebSocket(portId)
            startUpload(cr, data, portId.toInt())
        }
        requestAvailablePort(
            successCallback = successCallback,
            failureCallback = { t: Throwable -> Log.e(tag, t.message) }
        )
    }

    private fun startUpload(cr: ContentResolver?, data: Intent?, portId: Int) {
        Handler().postDelayed({ sendBitmapViaSocket(cr, data, portId) }, 100)
    }

    private fun openWebSocket(portId: Long) {
        val successCallback = { }
        val failureCallback = { t: Throwable -> Log.e(tag, t.message) }
        val callback = object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>,
                                    response: Response<JsonElement>
            ) {
                if (response.isSuccessful) {
                    if (response.body() != null) {
                        val model = response.body()
                        model?.let {
                            successCallback.invoke()
                            return
                        }

                        failureCallback.invoke(Throwable("No response returned."))
                    }
                } else {
                    failureCallback.invoke(Throwable(response.errorBody()?.string()))
                }
            }
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                t.printStackTrace()
                failureCallback.invoke(t)
            }
        }

        createWebSocketCall = adapter.openWebSocket(
            portId = portId,
            callback = callback)
        createWebSocketCall?.enqueue(callback)
    }

    private fun requestAvailablePort(
        successCallback: (portId: Long) -> Unit,
        failureCallback: (Throwable) -> Unit
    ) {
        val callback = object : Callback<Long> {
            override fun onResponse(call: Call<Long>,
                                    response: Response<Long>
            ) {
                if (response.isSuccessful) {
                    if (response.body() != null) {
                        val portId = response.body()
                        portId?.let {
                            successCallback.invoke(it)
                            return
                        }

                        failureCallback.invoke(Throwable("No port ID was returned."))
                    }
                } else {
                    failureCallback.invoke(Throwable(response.errorBody()?.string()))
                }
            }
            override fun onFailure(call: Call<Long>, t: Throwable) {
                t.printStackTrace()
                failureCallback.invoke(t)
            }
        }

        adapter.requestAvailablePort(callback = callback)
    }

    private fun sendBitmapViaSocket(cr: ContentResolver?,
                                    data: Intent?,
                                    portId: Int) {
        var bm: Bitmap? = null

        data?.extras?.let {
            bm = it.get("data") as Bitmap
        }
        data?.data?.let {
            bm = MediaStore.Images.Media.getBitmap(cr, it)
        }
        bm?.let {
            val bao = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 100, bao)
            val ba = bao.toByteArray()
            val size = ba.size
            val inputStream = ByteArrayInputStream(ba)
            sendDataViaSocket(portId, size, inputStream)
            if(createWebSocketCall?.isExecuted == false) {
                createWebSocketCall?.cancel()
            }
        }
    }

    private fun sendDataViaSocket(
        portId: Int,
        size: Int,
        inputStream: ByteArrayInputStream
    ) {
        val hostName = "7eac23d1.ngrok.io"
        var socket: Socket? = null
        val fileName = System.currentTimeMillis().toString()

        try {
            socket = Socket(hostName, portId)

            val dis = DataInputStream(BufferedInputStream(socket.getInputStream()))

            DataOutputStream(
                BufferedOutputStream(socket.getOutputStream())
            ).use { dos ->
                dos.writeUTF(System.currentTimeMillis().toString())
                dos.writeUTF(".png")
                dos.writeLong(size.toLong())
                writeFile(inputStream, dis, dos)
                Log.i(
                    tag,
                    "IP=[${socket.inetAddress}] port=[$portId] fileName=[$fileName] fileSize=[${size.toLong()}]"
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (socket != null) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun writeFile(reader: ByteArrayInputStream, inputStream: DataInputStream, outStream: OutputStream) {
        val bufferSize = 4096
        try {
            val buffer = ByteArray(bufferSize)
            var pos = 0
            var bytesRead: Int = reader.read(buffer, 0, bufferSize)
            while (bytesRead >= 0) {
                Log.i(tag, "sending bytesRead bytes...")
                outStream.write(buffer, 0, bytesRead)
                outStream.flush()
                pos += bytesRead
                bytesRead = reader.read(buffer, 0, bufferSize)
            }

            val response = inputStream.read()
            Log.i(tag, "response=$response")

//            Log.i(tag, "response=[${String.format("%05X", response and 0xFFFFF)}]")
        } catch (e: IndexOutOfBoundsException) {
            e.message?.let { Log.i(tag,it) }
            e.printStackTrace()
        } catch (e: IOException) {
            e.message?.let { Log.i(tag,it) }
            e.printStackTrace()
        } finally {
            try {
                reader.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun addRecordToDatabase(
        successCallback: () -> Unit,
        failureCallback: (Throwable) -> Unit) {

    }
}
