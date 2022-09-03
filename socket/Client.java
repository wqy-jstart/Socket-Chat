package socket;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 聊天室客户端
 */
public class Client {
    /*
     *java.net.Socket
     * Socket封装了TCP协议的通讯细节,使用它就可以和远端计算机建立TCP连接,并基于
     * 两条流(一个输出一个输入流)与远端计算机交互数据
     */
    private Socket socket;
    public Client(){
        try {//176.24.6.210
            /*
                实例化Socket就是与远端计算机建立连接的过程。
                这里参数1:服务端计算机的IP地址，参数2:服务端打开的服务端口
                我们通过IP找到服务器计算机通过端口连接到服务器上的服务端应用程序。
             */
            System.out.println("正在连接服务端...");
            //Connection refused拒绝连接,说明端口号未被占用
            socket = new Socket("localhost",8088);//服务器的IP和端口
            System.out.println("与服务端建立连接！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //工作区
    public void start(){
        try {
            //启动用来读取服务端消息的线程
            //将任务实例化
            ServerHandler handler = new ServerHandler();
            //创建线程,将任务传入
            Thread t = new Thread(handler);
            //启动线程,等待CPU分配时间片，启动run()方法
            t.setDaemon(true);//将该线程修改为守护线程
            t.start();

            //隐藏子类的名字,直接用超类引用去看待
            OutputStream out = socket.getOutputStream();//用socket获取输出流
            OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);//转换输出流
            BufferedWriter bw = new BufferedWriter(osw);//缓冲字符输出流
            PrintWriter pw = new PrintWriter(bw,true);//行刷新输出流


            Scanner scanner = new Scanner(System.in);
            System.out.println("开始聊天吧！");
            while (true){
                String line = scanner.nextLine();
                if ("exit".equalsIgnoreCase(line)){
                    break;
                }
                pw.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭socket时会与服务端进行挥手操作
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }

    /**
     * 该线程负责读取服务端发送过来的消息
     */
    private class ServerHandler implements Runnable{
        @Override
        public void run() {
            try {
                //通过socket获取输入流读取服务端发送过来的消息
                InputStream in = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(in,StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String message;
                while ((message = br.readLine()) !=null){
                    System.out.println(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
