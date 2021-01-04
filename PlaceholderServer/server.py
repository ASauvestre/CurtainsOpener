import socket, time

# State
enabled = 0
override_enabled = 0
override_time = 0
override_length = 0

day_enabled = [0] * 7
day_time = [0] * 7
day_length = [0] * 7


def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("10.255.255.255", 80))
    return s.getsockname()[0]

def wait_for_client():
    clientsocket, (client_ip, client_port) = serversocket.accept()
    print("Connection address: " + client_ip + ":" + str(client_port))
    return clientsocket

serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serversocket.bind((get_local_ip(), 4242))
serversocket.listen(1)

clientsocket = wait_for_client()

while clientsocket:
    packet_length = int.from_bytes(clientsocket.recv(1), "big")

    if packet_length == 0: # Socket was closed
        clientsocket = wait_for_client()
    else :
        data_length = packet_length - 3
        version = int.from_bytes(clientsocket.recv(1), "big")
        command = int.from_bytes(clientsocket.recv(1), "big")
        if data_length :
            data = clientsocket.recv(data_length)

            if command < 40:
                print("Received packet: Data Length = " + str(data_length) + " Version = " + str(version) + " Command = " + str(command) + " Data = " + str(int.from_bytes(data, "big")))
            else:
                print("Received packet: Data Length = " + str(data_length) + " Version = " + str(version) + " Command = " + str(command) + " Data = " + str(data[0]) + ", " + str(int.from_bytes(data[1:], "big")))

            if command == 20:
                enabled = int.from_bytes(data, "big")

            if command == 30:
                override_enabled = int.from_bytes(data, "big")
            if command == 31:
                override_time = int.from_bytes(data, "big")
            if command == 32:
                override_length = int.from_bytes(data, "big")

            if command == 40:
                index = data[0]
                day_enabled[index] = int.from_bytes(data[1:], "big")
            if command == 41:
                index = data[0]
                day_time[index] = int.from_bytes(data[1:], "big")
            if command == 42:
                index = data[0]
                day_length[index] = int.from_bytes(data[1:], "big")


        else:
            print("Received packet: Data Length = " + str(data_length) + " Version = " + str(version) + " Command = " + str(command))

        if command == 2 or command == 3:
            response_length = 76
            response = bytearray()
            response += int.to_bytes(response_length, 1, "big")
            response += int.to_bytes(version, 1, "big")
            response += int.to_bytes(command, 1, "big")
            response += int.to_bytes(enabled, 1, "big")
            response += int.to_bytes(override_enabled, 1, "big")
            response += int.to_bytes(override_time, 4, "big")
            response += int.to_bytes(override_length, 4, "big")

            for i in range(7):
                response += int.to_bytes(day_enabled[i], 1, "big")
                response += int.to_bytes(day_time[i], 4, "big")
                response += int.to_bytes(day_length[i], 4, "big")
        else:
            response_length = 3
            response = bytearray()
            response += int.to_bytes(response_length, 1, "big")
            response += int.to_bytes(version, 1, "big")
            response += int.to_bytes(command, 1, "big")

        clientsocket.send(response)
