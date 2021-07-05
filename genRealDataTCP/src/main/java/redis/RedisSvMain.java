/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dat.chuthanh
 */
public class RedisSvMain {
    public static Integer doubleLineData = 10000;
    private static HashMap<String, String> map = new HashMap<String, String>();
    private static MsgQueueRedis msgQueueRedis = new MsgQueueRedis();
    public static Integer numMatching = 0;
    public static long startTime = 0;
    public static int executeCount = 0;
    public static long timeWait = 0;

//    public static PriorityBlockingQueue<String> queue = new PriorityBlockingQueue<String>(10000, new Comparator<String>() {
//        public int compare(String s1, String s2) {
//            for (int i = 0; i < 14; i++) {
//                if (s1.charAt(i) < s2.charAt(i + 8)) return -1;
//                else if (s1.charAt(i) > s2.charAt(i + 8)) return 1;
//            }
//            if (s1.startsWith("NVL01_1")) return 1;
//            else if (s2.startsWith("NVL01_1")) return -1;
//            return 0;
//        }
//    });

    public static PriorityBlockingQueue<String> queue = new PriorityBlockingQueue<String>(100000, (s1, s2) -> {
        int s1TimeIdx = 0, s2TimeIdx = 0;
        if (s1.startsWith("NVL01_1")) s1TimeIdx = 8;
        if (s2.startsWith("NVL01_1")) s2TimeIdx = 8;
        for (int i = 0; i < 14; i++) {
            if (s1.charAt(i + s1TimeIdx) < s2.charAt(i + s2TimeIdx)) return -1;
            else if (s1.charAt(i + s1TimeIdx) > s2.charAt(i + s2TimeIdx)) return 1;
        }
        if (s1.startsWith("NVL01_1")) return 1;
        else if (s2.startsWith("NVL01_1")) return -1;
        return 0;
    });

    public static void main(String[] args) {
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        ServerSocket serverSocket = new ServerSocket(11000);
                        System.out.println("Server TCP with port : " + 11000 + " is running...");
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("client was accpeted ");
                        System.out.println(clientSocket.getInetAddress());
                        ObjectInputStream os = new ObjectInputStream(clientSocket.getInputStream());
                        try {
                            while (true) {
                                String data = os.readObject().toString().trim();
                                String[] arrayData = data.split("\n");
                                queue.addAll(Arrays.asList(arrayData));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        ServerSocket serverSocket = new ServerSocket(11001);
                        System.out.println("Server TCP with port : " + 11001 + " is running...");
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("client was accpeted ");
                        System.out.println(clientSocket.getInetAddress());
                        ObjectInputStream os = new ObjectInputStream(clientSocket.getInputStream());
                        try {
                            while (true) {
                                String data = os.readObject().toString().trim();
                                String[] arrayData = data.split("\n");
                                queue.addAll(Arrays.asList(arrayData));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                try {
                    startTime = System.currentTimeMillis();
                    while (true) {
                        String data = queue.take();
                        if (data.startsWith("NVL01")) {
                            String[] dataArr = data.split(",");
                            String privateIp = dataArr[2].trim();

                            if (map.get(privateIp) != null){
                                String[] arrData1 = data.split(",");
                                String[] arrData2 = map.get(privateIp).split(",");
                                    String key = arrData2[3] + "_" + arrData1[4];
                                    String value = data + "," + map.get(privateIp);
                                    msgQueueRedis.addByKeyVlue(key, value);
                                    numMatching++;
                            }
                        } else {
                            String[] dataArr = data.split(",");
                            String ip = dataArr[4].trim();
                            if (dataArr[2].equals("Start")) map.put(ip, data);
                        }
                        //execute count
                        executeCount++;
                        if (executeCount == doubleLineData) {
                            long currTime = System.currentTimeMillis();
                            long timeExe = currTime - startTime;

                                System.out.println("time execute 1000 data : " + (timeExe+timeWait) + "ms");
                                System.out.println("numMatching " + numMatching);
                                System.out.println("--------------------------------------------");

                            numMatching = 0;
                            executeCount = 0;
                            startTime = System.currentTimeMillis();
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}

