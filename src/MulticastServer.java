import java.io.IOException;
import java.net.*;
import java.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

public class MulticastServer extends Thread implements Serializable {
    private String MULTICAST_ADDRESS = "224.0.224.0";
    private int PORT = 4321;
    private long SLEEP_TIME = 5000;
    private ArrayList<User> usersList = new ArrayList<>();

    public static void main(String[] args){
        MulticastServer server = new MulticastServer();
        server.start();
    }

    public MulticastServer(){ super ("Server " + (long) (Math.random()*1000));}

    public void run(){
        MulticastSocket socket = null;
        Runtime.getRuntime().addShutdownHook(new catchCtrlC(usersList));
        //System.out.println(this.getName() + "run...");

        try {
            socket = new MulticastSocket(PORT);
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            usersList = readFiles();
            for(User user : usersList) {
                if (user == null) {
                    System.out.println("rip");
                } else {
                    System.out.println(user);
                }
            }
            while(true){
                System.out.println("INICIO");
                byte[] bufferRec = new byte[256];
                DatagramPacket packetRec = new DatagramPacket(bufferRec, bufferRec.length);
                socket.setLoopbackMode(false);
                socket.receive(packetRec);
                System.out.print("De: "+ packetRec.getAddress().getHostAddress() + ":" + packetRec.getPort() + " com a mensagem: ");
                String msg = new String(packetRec.getData(), 0, packetRec.getLength());
                System.out.println(msg);
                //try { sleep((long) (Math.random() * SLEEP_TIME)); } catch (InterruptedException e) { }
                String[] aux = msg.split(";");
                switch (aux[0]){
                    case "type|login":
                        String [] loginUsernameParts = aux[1].split("\\|");
                        String [] loginPasswordParts = aux[2].split("\\|");
                        String user = loginUsernameParts[1];
                        String pass = loginPasswordParts[1];
                        System.out.println("USERNAME: " + user + " PASSWORD: " + pass);
                        boolean loggedInSuccessfully = checkUsernameLogin(user, pass);
                        if(loggedInSuccessfully == false){
                            sendMsg(socket,"type|loginFail");
                            System.out.println("ERRO: Login não completo.");
                        }
                        else{
                            sendMsg(socket,"type|loginComplete");
                            System.out.println("SUCESSO: Login Completo");
                        }
                        //funçao passa como argumentos o user e pw
                        //funçao pra confirmar se o user existe, se a pw ta certa e por fim enviar a resposta
                        break;
                    case "type|register":
                        String aux2 = aux[1];
                        String [] registerUsernameParts = aux2.split("\\|");
                        String [] registerPasswordParts = aux[2].split("\\|");
                        String username = registerUsernameParts[1];
                        String password = registerPasswordParts[1];
                        System.out.println("USERNAME: " + username + " PASSWORD: " + password);
                        boolean usernameUsed = checkUsernameRegister(username);
                        if(usernameUsed == true) {
                            sendMsg(socket, "type|usernameUsed");
                            System.out.println("ERRO: Username já usado.");
                        }
                        else {
                            User newUser = new User(username, password);
                            usersList.add(newUser);
                            System.out.println("SUCESSO: Adicionou ao arraylist com user '" + username + "' e password '" + password +"'");
                            sendMsg(socket, "type|registComplete");
                        }
                        //funçao passa como argumentos o user e pw
                        //na funçao verificar se nao ha users iguais, se nao guardar no arraylist (se usarmos 2 pws ver se sao iguais) e enviar a resposta
                        break;
                    case "type|pesquisa":
                        //dentro da funçao decidir o que pesquisa artista, estilo ou album
                        break;
                    default:
                        System.out.println("Default");
                        break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    public boolean checkUsernameLogin(String username, String password){
        for (User user : usersList) {
            if(user.getUsername().equals(username)){
                if(user.checkPassword(password)){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkUsernameRegister(String username){
        for (User user : usersList) {
            if(user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    private void sendMsg(MulticastSocket socket, String msg) throws IOException {
        byte[] buffer = msg.getBytes();
        socket.setLoopbackMode(true);

        InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
        socket.send(packet);
    }

    private ArrayList<User> readFiles(){
        System.out.println("A ler");
        ArrayList<User> users = new ArrayList<>();
        try {
            ObjectInputStream objectIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream("data.bin")));
            users = (ArrayList) objectIn.readObject();
            objectIn.close();
            return users;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static void writeFiles(ArrayList<User> usersList){
        System.out.println("Entrou");
        try{
            ArrayList<User> teste = new ArrayList<>();
            teste.add(new User("Joao","caralho"));
            teste.add(new User("franc","caralho"));
            teste.add(new User("nelso","caralho"));
            File file = new File("data.bin");
            FileOutputStream out = new FileOutputStream(file);
            ObjectOutputStream fout = new ObjectOutputStream(out);
            //fout.writeObject(usersList);
            fout.writeObject(teste);
            //fout.writeObject("continue");
            fout.close();
            out.close();
        }
        catch (FileNotFoundException ex) {
            Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("File not found");
        } catch (IOException ex) {
            Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class catchCtrlC extends Thread {
    ArrayList<User> users = new ArrayList<>();
    public catchCtrlC(ArrayList<User> usersList) {
        this.users = usersList;
    }

    @Override
    public void run() {
        for(User user : users) {
            if (user == null) {
                System.out.println("rip");
            } else {
                System.out.println(user);

            }
        }
        MulticastServer.writeFiles(users);
        System.out.println("Escreveu no ficheiro de objetos.");
    }
}