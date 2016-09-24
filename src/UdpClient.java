import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import processing.core.PApplet;
import processing.data.JSONObject;

class UdpClient extends PApplet {
    SocketAddress arduinoAddress;
    DatagramPacket incomingUdp;
    DatagramSocket udpSocket;
    String address;
    
    public UdpClient(int arduinoPort, int receivePort) {
        // configure send port 
        String ip = "127.0.0.1";
        arduinoAddress = new InetSocketAddress("127.0.0.1",arduinoPort);
        this.address = ip + ":" + receivePort;

        try {
            udpSocket = new DatagramSocket(receivePort);
            udpSocket.setSoTimeout(1);
            byte[] receiveData = new byte[1024];
            incomingUdp = new DatagramPacket(receiveData, receiveData.length);
        } catch (IOException e) {
            println(e);
            println("exiting in setup udp receiver");
            System.exit(0); 
        }
    }

    public UdpClient(String ip, int arduinoPort, int receivePort) {
        // configure send port 
        arduinoAddress = new InetSocketAddress(ip,arduinoPort);
        this.address = ip + ":" + receivePort;

        try {
            udpSocket = new DatagramSocket(receivePort);
            udpSocket.setSoTimeout(1);
            byte[] receiveData = new byte[1024];
            incomingUdp = new DatagramPacket(receiveData, receiveData.length);
        } catch (IOException e) {
            println(e);
            println("exiting in setup udp receiver");
            System.exit(0); 
        }
    }

    public UdpClient(String ip, int arduinoPort) {
        // configure send port 
        arduinoAddress = new InetSocketAddress(ip,arduinoPort);
        this.address = ip;
        try {
            udpSocket = new DatagramSocket(null);
        } catch (IOException e) {
            println(e);
            println("error createing unbound socket");
        }
    }


    void sendMessage(String message) {
      message = message.replaceAll("[\r|\n|\\s]", "");
      println("attempting send [" + arduinoAddress + "]: " + message);
      try {
          byte[] sendData = message.getBytes("UTF-8");
          DatagramPacket sendPacket = new DatagramPacket(sendData, 0,
            sendData.length, arduinoAddress);
          udpSocket.send(sendPacket);
      } catch (IOException e) {
        println(e);
        println("error sending packet");
      }
    }

    boolean receiveMessage(JSONBuffer json) {
        try {
            byte[] receiveData = new byte[1024];
            udpSocket.receive(incomingUdp);
            String message = new String(
                incomingUdp.getData(), 0, incomingUdp.getLength());
            //println("[" + this.address + "] " + message);
            json.json = new JSONObject();
            json.json.setJSONObject(this.address, JSONObject.parse(message));

            return true;
        } catch (IOException e) {
        // no new messages recieved, return false
        }

        return false;
    }

    void flushBuffer() {
        JSONBuffer json = new JSONBuffer();
        while (receiveMessage(json)) {}
    }

    void closeSocket() {
        udpSocket.close();
        delay(100);
    }
}
