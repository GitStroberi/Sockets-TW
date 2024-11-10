package run_time_db;

import packet.Command;
import packet.Packet;
import packet.User;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public enum UserManagement {
    INSTANCE;

    public List<User> users;
    public Map<String, List<User>> chatRooms = new HashMap<>(); // Chat rooms

    UserManagement() {
        this.users = List.of(
                User.builder().nickname("Mock 1").password("1234").build(),
                User.builder().nickname("Mock 2").password("1234").build(),
                User.builder().nickname("Mock 3").password("1234").build(),
                User.builder().nickname("Mock 4").password("1234").build()
        );

        chatRooms.put("asd", new ArrayList<>());
        chatRooms.put("General", new ArrayList<>());
        chatRooms.put("Random", new ArrayList<>());
    }

    public void addUserToRoom(String roomName, User user) {
        chatRooms.get(roomName).add(user);
    }

    public void sendRoomMessage(Packet packet) {
        String roomName = packet.getRoom();
        if (!chatRooms.get(roomName).contains(packet.getUser())) {
            return;
        }
        users.stream()
                .filter(user -> chatRooms.get(roomName).contains(user) &&
                        !user.getNickname().equals(packet.getUser().getNickname()))
                .forEach(user -> sendMessageToUser(user, packet));
    }

    public void sendPrivateMessage(Packet packet) {
        String recipientNickname = packet.getRecipient();
        users.stream()
                .filter(user -> user.getNickname().equals(recipientNickname))
                .findFirst()
                .ifPresent(user -> sendMessageToUser(user, packet));
    }

    public void sendMessageToUser(User user, Packet packet) {
        try {
            if (user.getOutStream() != null) {
                user.getOutStream().writeObject(packet);
                user.getOutStream().flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error sending message to user: " + user.getNickname(), e);
        }
    }

    public void joinRoom(Packet packet) {
        String roomName = packet.getRoom();
        User user = packet.getUser();
        addUserToRoom(roomName, user);
    }

    public void register() {
        /* TODO: Implement */
    }

    public Optional<User> login(User userToLogin) {
        return this.users
                .stream()
                .filter(user -> user.equals(userToLogin))
                .findFirst();
    }

    public void broadcastMessage(Packet packet) {
        for (User user : users) {
            /* Do not send the message back to the user who sent it */
            boolean isNotSameUser = !packet.getUser().getNickname().equals(user.getNickname());

            if (Objects.nonNull(user.getSocket()) && isNotSameUser) {
                User cleanUser = User.builder()
                        .nickname(packet.getUser().getNickname())
                        .build();

                Packet messagePacket = Packet.builder()
                        .user(cleanUser)
                        .message(packet.getMessage())
                        .command(Command.MESSAGE_ALL)
                        .build();

                try {
                    ObjectOutputStream userOutStream = user.getOutStream();
                    if (userOutStream != null) {
                        userOutStream.writeObject(messagePacket);  // Use the user's existing stream
                        userOutStream.flush();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error sending message to user: " + user.getNickname(), e);
                }
            }
        }
    }
}