#include "bluetooth.h"

// Bluetooth Variables
int bt_socket, client;
struct sockaddr_rc bt_server = { 0 }, bt_client = { 0 };
char buf[BUFFER_SIZE] = { 0 };
socklen_t opt = sizeof(bt_client);

void setupBluetooth(){

	printf("[Bluetooth] Restarting Interface\n");
	system("sudo /etc/init.d/bluetooth restart");

	printf("[Bluetooth] Setting Up\n");

	// 1. Bluetooth Adapter
	bt_server.rc_family = AF_BLUETOOTH;
	bt_server.rc_bdaddr = *BDADDR_ANY;
	bt_server.rc_channel = (uint8_t) BLUETOOTH_PORT;

	// 2. Allocate Socket
	bt_socket = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);

	// 3. Bind Socket to Port
	bind(bt_socket, (struct sockaddr *)&bt_server, sizeof(bt_server));

	// 4. Put Socket into Listening Mode
	listen(bt_socket, 1);
	printf("[Bluetooth] Waiting for Client\n");

	// 5. Accept One Connection
	client = accept(bt_socket, (struct sockaddr *)&bt_client, &opt);
	//ba2str( &bt_client.rc_bdaddr, buf );
	
	printf("[Bluetooth] Connected\n");
	bt_isConnected = 1;
}


//Added for reconnection in case the Android disconnects from RPi
void *bluetooth_reconnect(){

	while (1)
	{
		if(!bt_isConnected){
			closeBluetooth();
			usleep(5000000);
			setupBluetooth();
		}
	}
}

void *bluetooth_read(){
	
	int bytes_read;

	do{
		bytes_read = recv(client,buf,sizeof(buf),0);
		if(bytes_read > 0) {
			strncpy(input, buf, sizeof(buf));
			bzero(buf, BUFFER_SIZE);
			sender = 'B';
			rpi_hasReceived = 1;
			//fprintf(stdout, "[Bluetooth] Read: %s (%d Bytes)\n", buf, bytes_read);
		}else{
			bt_isConnected = 0;	
		}
	}while(1);
}

void *bluetooth_write(){
	
	int status;

	do{
		if(bt_isWriting){
			strtok(output, "\n");
			strtok(output, "\r");
			status = send(client, output, strlen(output), 0);
			if(status > 0) {
				bt_isWriting = 0;
				//fprintf(stdout,"[Bluetooth] Sent: %s (%d Bytes)\n", output, status);
			}else{
				bt_isConnected = 0;
			}
		}
	}while(1);
}

void closeBluetooth(){
	close(client);
	close(bt_socket);
}