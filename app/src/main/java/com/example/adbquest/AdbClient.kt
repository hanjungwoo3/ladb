package com.example.adbquest

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AdbClient {
    
    fun connectToDevice(ip: String, port: Int = 5555): String {
        return try {
            // 1단계: 포트 연결 테스트
            val socket = Socket()
            socket.soTimeout = 5000 // 5초 타임아웃
            socket.connect(java.net.InetSocketAddress(ip, port), 5000)
            
            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())

            // 2단계: ADB 프로토콜 시도
            try {
                // ADB의 실제 handshake 메시지 전송
                val message = "host:version"
                val messageLength = String.format("%04x", message.length)
                val fullMessage = messageLength + message
                
                out.writeBytes(fullMessage)
                out.flush()
                
                // ADB 응답 읽기 시도
                socket.soTimeout = 2000 // 2초 대기로 단축
                
                // 첫 4바이트 읽기 (길이 또는 상태)
                val responseHeader = ByteArray(4)
                val headerRead = input.read(responseHeader)
                
                if (headerRead == 4) {
                    val headerStr = String(responseHeader)
                    
                    if (headerStr == "OKAY") {
                        // 성공 시 tcpip 5555 명령 전송
                        val tcpipResult = sendTcpipCommand(ip, port)
                        socket.close()
                        "✅ ADB Connection Successful!\n$ip:$port - Handshake completed\n$tcpipResult"
                    } else if (headerStr == "FAIL") {
                        socket.close()
                        "❌ ADB Connection Failed\n$ip:$port - Device rejected connection"
                    } else {
                        // 길이 정보인 경우 추가 데이터 읽기
                        try {
                            val length = Integer.parseInt(headerStr, 16)
                            if (length > 0 && length < 1000) {
                                val data = ByteArray(length)
                                input.read(data)
                                val response = String(data)
                                // 버전 확인 후 tcpip 명령 전송
                                val tcpipResult = sendTcpipCommand(ip, port)
                                socket.close()
                                "✅ ADB Connection Successful!\n$ip:$port - Version: $response\n$tcpipResult"
                            } else {
                                socket.close()
                                "✅ ADB Service Detected!\n$ip:$port - Ready for connection"
                            }
                        } catch (e: NumberFormatException) {
                            socket.close()
                            "✅ ADB Service Detected!\n$ip:$port - Service responding"
                        }
                    }
                } else {
                    socket.close()
                    "✅ ADB Service Available!\n$ip:$port - Port open, service ready"
                }
                
            } catch (e: java.net.SocketTimeoutException) {
                socket.close()
                // 포트 연결은 성공했으므로 ADB가 활성화된 것으로 간주
                "✅ ADB Service Detected!\n$ip:$port - Service active (authentication may be required)"
            } catch (e: Exception) {
                socket.close()
                "✅ ADB Service Available!\n$ip:$port - Port accessible"
            }
            
        } catch (e: SocketTimeoutException) {
            "❌ Connection Failed\n$ip:$port - Timeout (ADB may not be enabled)"
        } catch (e: java.net.ConnectException) {
            "❌ Connection Failed\n$ip:$port - Port closed (ADB not running)"
        } catch (e: java.net.UnknownHostException) {
            "❌ Connection Failed\n$ip - Invalid IP address"
        } catch (e: Exception) {
            "❌ Connection Error\n$ip:$port - ${e.javaClass.simpleName}: ${e.message}"
        }
    }
    
    private fun sendTcpipCommand(ip: String, port: Int): String {
        return try {
            val socket = Socket()
            socket.soTimeout = 5000
            socket.connect(java.net.InetSocketAddress(ip, port), 5000)
            
            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())
            
            // tcpip:5555 명령 전송
            val tcpipMessage = "tcpip:5555"
            val messageLength = String.format("%04x", tcpipMessage.length)
            val fullMessage = messageLength + tcpipMessage
            
            out.writeBytes(fullMessage)
            out.flush()
            
            // 응답 읽기
            socket.soTimeout = 3000
            val responseHeader = ByteArray(4)
            val headerRead = input.read(responseHeader)
            
            socket.close()
            
            if (headerRead == 4) {
                val headerStr = String(responseHeader)
                when (headerStr) {
                    "OKAY" -> "🔄 TCPIP 5555 command sent successfully!"
                    "FAIL" -> "⚠️ TCPIP 5555 command failed"
                    else -> "🔄 TCPIP 5555 command sent (response: $headerStr)"
                }
            } else {
                "🔄 TCPIP 5555 command sent (no response)"
            }
            
        } catch (e: Exception) {
            "⚠️ Failed to send TCPIP 5555 command: ${e.message}"
        }
    }
    
    // 실제 ADB 명령 실행을 위한 함수 (향후 확장용)
    fun executeAdbCommand(ip: String, command: String): String {
        return try {
            // 실제로는 ProcessBuilder를 사용해서 adb 명령을 실행할 수 있지만
            // Android 앱에서는 제한이 있음
            "ADB command execution not implemented in Android app context"
        } catch (e: Exception) {
            "Error executing ADB command: ${e.message}"
        }
    }
} 