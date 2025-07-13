package com.example.adbquest

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import android.net.wifi.WifiManager
import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import java.net.InetAddress
import android.content.SharedPreferences
import kotlinx.coroutines.joinAll
import java.util.Collections
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var ipEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var scanButton: Button
    private lateinit var ipListView: ListView
    private val foundIps = mutableListOf<String>()
    private lateinit var prefs: SharedPreferences
    private var savedMac: String? = null
    private val foundIpMacs = mutableListOf<Pair<String, String>>()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 권한 체크 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_REQUEST_CODE)
        }

        ipEditText = findViewById(R.id.ipEditText)
        connectButton = findViewById(R.id.connectButton)
        resultTextView = findViewById(R.id.resultTextView)
        scanButton = findViewById(R.id.scanButton)
        ipListView = findViewById(R.id.ipListView)

        prefs = getSharedPreferences("adbquest", Context.MODE_PRIVATE)
        savedMac = prefs.getString("quest_mac", null)

        connectButton.setOnClickListener {
            val ip = ipEditText.text.toString()
            if (ip.isNotEmpty()) {
                resultTextView.text = "Connecting..."
                GlobalScope.launch(Dispatchers.IO) {
                    val result = AdbClient.connectToDevice(ip)
                    withContext(Dispatchers.Main) {
                        resultTextView.text = result
                    }
                }
            }
        }

        scanButton.setOnClickListener {
            resultTextView.text = "네트워크 스캔 중..."
            foundIpMacs.clear()
            ipListView.visibility = View.GONE
            GlobalScope.launch(Dispatchers.IO) {
                val ipMacs = scanLocalNetworkWithMac()
                withContext(Dispatchers.Main) {
                    if (ipMacs.isNotEmpty()) {
                        foundIpMacs.addAll(ipMacs)
                        val displayList = ipMacs.map { "${it.first} (${it.second})" }
                        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, displayList)
                        ipListView.adapter = adapter
                        ipListView.visibility = View.VISIBLE
                        // 저장된 MAC이 있으면 자동 선택
                        val auto = savedMac?.let { mac -> ipMacs.find { it.second.equals(mac, true) } }
                        if (auto != null) {
                            ipEditText.setText(auto.first)
                            resultTextView.text = "저장된 Quest IP 자동 선택: ${auto.first}"
                        } else {
                            resultTextView.text = "기기 선택 또는 직접 입력"
                        }
                    } else {
                        resultTextView.text = "기기를 찾지 못했습니다."
                    }
                }
            }
        }

        ipListView.setOnItemClickListener { _, _, position, _ ->
            val (ip, mac) = foundIpMacs[position]
            ipEditText.setText(ip)
            ipListView.visibility = View.GONE
            // MAC 저장
            prefs.edit().putString("quest_mac", mac).apply()
            savedMac = mac
            resultTextView.text = "Quest MAC 저장됨: $mac"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용됨
            } else {
                Toast.makeText(this, "위치 권한이 없으면 네트워크 탐색이 불가합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun scanLocalNetwork(): List<String> {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wifiManager.connectionInfo.ipAddress
        val ipString = String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
        val subnet = ipString.substringBeforeLast(".")
        val found = mutableListOf<String>()
        val port = 5555
        val jobs = mutableListOf<Job>()
        val dispatcher = Dispatchers.IO
        val scope = CoroutineScope(dispatcher)
        val lock = java.lang.Object()
        for (i in 2..254) {
            val host = "$subnet.$i"
            val job = scope.launch {
                try {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(host, port), 150)
                    socket.close()
                    synchronized(lock) { found.add(host) }
                } catch (_: Exception) {}
            }
            jobs.add(job)
        }
        runBlocking { jobs.forEach { it.join() } }
        return found
    }

    private suspend fun scanLocalNetworkWithMac(): List<Pair<String, String>> {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wifiManager.connectionInfo.ipAddress
        val ipString = String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
        val subnet = ipString.substringBeforeLast(".")
        val found = Collections.synchronizedList(mutableListOf<Triple<String, String, String>>())
        val port = 5555
        val dispatcher = Dispatchers.IO
        // 비동기 스캔
        coroutineScope {
            (1..255).map { i ->
                launch(dispatcher) {
                    val host = "$subnet.$i"
                    val start = System.currentTimeMillis()
                    try {
                        java.net.Socket().use { socket ->
                            socket.connect(java.net.InetSocketAddress(host, port), 150)
                            val latency = System.currentTimeMillis() - start
                            val hostName = try {
                                java.net.InetAddress.getByName(host).hostName
                            } catch (_: Exception) { "" }
                            found.add(Triple(host, "${latency}ms", hostName))
                        }
                    } catch (_: Exception) {}
                }
            }.joinAll()
        }
        // 리스트에 IP, 응답시간, 호스트명 표시
        val ipInfos = mutableListOf<Pair<String, String>>()
        for ((ip, latency, hostName) in found) {
            val info = if (hostName.isNotBlank() && hostName != ip) "$latency, $hostName" else latency
            ipInfos.add(ip to info)
        }
        return ipInfos
    }
} 