#include "arduino.h"

char buffer[BUFFER_SIZE] = "";
char temp[BUFFER_SIZE] = "";
int fd, n, i;
int arduino_isWaiting = 0;

void setupArduino(){
	struct termios toptions;

	printf("[Arduino] Setting Up\n");

  	/* open serial port */
	fd = open("/dev/ttyACM0", O_RDWR | O_NOCTTY | O_NDELAY);
	
	// Check Serial Connection Status
	if (fd != -1){

  		/* wait for the Arduino to reboot */
		usleep(3000000);

  		/* get current serial port settings */
		if (tcgetattr(fd, &toptions) < 0) {
			printf("[Arduino] Couldn't get term attributes\n");
		}
		
  		/* set 115200 baud both ways */
		cfsetispeed(&toptions, B115200);
		cfsetospeed(&toptions, B115200);

  		/* 8 bits, no parity, no stop bits */
		toptions.c_cflag &= ~PARENB;
		toptions.c_cflag &= ~CSTOPB;
		toptions.c_cflag &= ~CSIZE;
		toptions.c_cflag |= CS8;
		toptions.c_cflag &= ~CRTSCTS;
		toptions.c_cflag |= CREAD | CLOCAL;
		toptions.c_iflag &= ~(IXON | IXOFF | IXANY);
		toptions.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
		toptions.c_oflag &= ~OPOST;
		toptions.c_cc[VMIN]  = 0;
		toptions.c_cc[VTIME] = 100;

		usleep(2000000); //required to make flush work, for some reason
		tcflush(fd,TCIOFLUSH);

  		/* commit the serial port settings */
		if (tcsetattr(fd, TCSAFLUSH, &toptions) < 0) {
			printf("[Arduino] Couldn't set term attributes\n");
		}

		printf("[Arduino] Connected\n");
	}else{
		printf("[Arduino] Unable to open Serial Port\n");
	}
}

void *arduino_read(){
	int size = 0;
	do{
		n  = read(fd, buffer, BUFFER_SIZE);
		if(n <= 0) continue;

        // Append Temp
		size += n;
		strcat(temp, buffer);
		bzero(buffer, BUFFER_SIZE);

		if(temp[size-1] != '\0'){
			continue;
		}

		if(arduino_isWaiting){
			strtok(temp, "\n");
			memset(input, '\0', sizeof(input));
			printf("[Arduino] Read: %s (%d) - %d\n", temp, strlen(temp), n);
			strncpy(input, temp, size);
			bzero(temp, BUFFER_SIZE);
			size = 0;
			sender = 'A';
			arduino_isWaiting = 0;
			rpi_hasReceived = 1;
		}
	}while(1);
}

void *arduino_write(){
	do{
		if(arduino_isWriting && !arduino_isWaiting){
			int status = write(fd, output, BUFFER_SIZE);
			if(status != -1) {
				arduino_isWriting = 0;
				arduino_isWaiting = 1;
			}
			printf("[Arduino] Sent: %s (%d) - %d\n", output, strlen(output), status);
		}
	}while(1);

	return 0;
}

void closeArduino(){
	usleep(2000000); //required to make flush work, for some reason
	tcflush(fd,TCIOFLUSH);
	close(fd);
}