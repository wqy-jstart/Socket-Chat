package socket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天室服务端(要用多线程处理多个客户端)
 */
public class Server {

    private ServerSocket serverSocket;
    //该集合存放的所有客户端的输出流用于广播消息
    private List<PrintWriter> allOut = new ArrayList<>();

    public Server() {
        try {
            System.out.println("正在启动服务端...");
            /*
                ★创建ServerSocket的同时要申请服务端口，该端口不能与系统其他程序
                开启的端口一致，否则会抛出异常:
                java.io.BindException:address already in use
                解决办法:
                更换端口
                或
                杀死占用该端口的程序进程:实际开发中8088很少被占用，通常都是由于我们
                启动了两次服务端导致的(第一次启动已经占用了8088，那么第二次启动时
                端口会显示被占用)

             */
            serverSocket = new ServerSocket(8088);//申请服务端口
            System.out.println("服务端启动完毕！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            while (true) {
                System.out.println("等待客户端连接...");
            /*
                ServerSocket提供的一个重要方法:
                Socket accept()-----调用该方法后用Socket引用类型来接收
                该方法是一个阻塞方法，调用会程序会卡住，直到一个客户端与服务端建立连接
                为止，此时返回的Socket就可与连接的客户端进行交互了。
             */
                Socket socket = serverSocket.accept();//等接电话
                System.out.println("一个客户端连接了！");

                //启动一个线程处理与该客户端交互
                //先把任务实例化,将socket传给对象,相当于把电话传给人接听
                ClientHandler handler = new ClientHandler(socket);//将socket传给对象
                //创建线程,传入实例化任务
                Thread t = new Thread(handler);
                t.start();//启动线程,随之执行run方法
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        Server server = new Server();//实例化Server(服务器)
        server.start();
    }

    /**
     * 将处理客户端交互的操作在一个单独的线程上进行,这里的主要工作就是与某个客户端交互.
     */
    //ClientHandler为内部类继承Runnable构成线程,其访问权限有四种
    private class ClientHandler implements Runnable{
        private Socket socket;//声明私有成员变量
        private String host;//记录客户端的IP地址信息

        public ClientHandler(Socket socket) {//因为new的时候传了一个参数,故此步需要传进构造方法中
            this.socket=socket;//用this关键字来区分成员变量与局部变量
            host = socket.getInetAddress().getHostAddress();
        }

        @Override
        //重写run方法,调用start()方法后等待CPU分配时间片后执行
        public void run() {
            PrintWriter pw = null;
            try {
            /*
                通过Socket获取输入流可以读取来自远端计算机发送过来的消息
             */
                InputStream in = socket.getInputStream();//通过socket获取输入流
                InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                //通过socket获取输出流用于给客户端发送信息
                OutputStream out = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(out,StandardCharsets.UTF_8);
                BufferedWriter bw = new BufferedWriter(osw);
                pw = new PrintWriter(bw,true);
                //将输出流存入共享集合allOut
                synchronized (allOut) {
                    allOut.add(pw);//分别添加客户端
                }
                System.out.println(host+"上线了,当前人数："+allOut.size());

                    String message;
            /*
                这里循环读取客户端发送过来消息这里可能出现下面的异常:
                java.net.SocketException: Connection reset
                这个错误是由于远端异常断开导致的(没有进行TCP的挥手断开操作)，该异常无法通过
                逻辑避免。
             */
                    while ((message = br.readLine()) != null) {
                        System.out.println(host + "说：" + message);
                        //将消息回复给客户端
                        synchronized (allOut){
                        for (PrintWriter o : allOut) {
                            o.println(host + "说：" + message);
                        }
                        }
                    }

            } catch (IOException e) {
//                e.printStackTrace();//输出错误堆栈信息,便于定位问题出现的位置和原因
                System.out.println("有一个客户端异常断开了！！！");
                String errormessage = e.getMessage();//可以获取异常的信息
                System.out.println("错误来源："+errormessage);//输出错误信息
            }finally {
                //处理客户端断开后的操作
                synchronized (allOut) {
                    allOut.remove(pw);//将当前客户端输出流从共享集合中删除
                }
                System.out.println(host+"下线了,当前人数："+allOut.size());
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
