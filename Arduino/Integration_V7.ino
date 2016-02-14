#include "DualVNH5019MotorShield.h"
#include "PinChangeInt.h"
#include <math.h>

DualVNH5019MotorShield md;

//Infrared Sensors
#define irL A5
#define irFL A4
#define irFR A3
#define irRF A2
#define irRB A1
#define irMid A0

//sent values
char sensorLS = '0';
char sensorFLS = '0';
char sensorFRS = '0';
char sensorRFS = '0';
char sensorRBS = '0';
char sensorMidS = '0';

//Unclassified 
boolean timeOut=false;
int state=0;
String txtMsg = "";
char s;
//Pins that are input for motors must not be used as interrupt pins.
//Encoder handling
#define motorL_A 11
#define motorL_B 5
#define motorR_A 3
#define motorR_B 13
volatile int motorL_oldA=0,motorR_oldA=0,motorL_newB=0,motorR_newB=0;
volatile long encoderLeft=0,encoderRight=0;


//#############################
//TESTED AT BATTERY POWER: 6.2
// Every time Calliberation required estimationg of:
// Kp, Kd(mostly) and finally Ki(v.v. small so do this in the end) parameters
// SAMPLETIME
//#############################

//PID
double rightPWM, leftPWM;
double dError=0,error=0,oldError=0,sError=0,outputF=0;
double cT=0,lT=0; //current and last time
int dirLeft=-1,dirRight=-1;  //-ve forward, +ve backward
#define SAMPLETIME 102

//Speeds
double leftS=0,rightS=0;
double lastCountLeft=0,lastCountRight=0;
double currentTime=0,lastTime=0;

//Distance moved
double prevTicks=0, prevTicksR=0,prevTime=0,distanceMoved=0;

//Rotation
double prevRotErrorR=0, prevRotErrorL=0;

//
double Kfp=3.45;

void setup(){
  Serial.begin(115200); 
  Serial.flush();
  md.init();

//  leftPWM=300;
//  rightPWM=300;

  //Encoders
  pinMode(motorL_A, INPUT);
  pinMode(motorL_B, INPUT);
  pinMode(motorR_A, INPUT);
  pinMode(motorR_B, INPUT);
  digitalWrite(motorL_A,HIGH);
  digitalWrite(motorL_B,HIGH);
  digitalWrite(motorR_A,HIGH);
  digitalWrite(motorR_B,HIGH);

  PCintPort::attachInterrupt(motorL_A, MotorLeftA, CHANGE);
  PCintPort::attachInterrupt(motorL_B, MotorLeftB, CHANGE);
  PCintPort::attachInterrupt(motorR_A, MotorRightA, CHANGE);
  PCintPort::attachInterrupt(motorR_B, MotorRightB, CHANGE);

  //IR Sensors
  pinMode(irL,INPUT);
  pinMode(irFL,INPUT);
  pinMode(irFR,INPUT);
  pinMode(irRF,INPUT);
  pinMode(irRB,INPUT);
  pinMode(irMid,INPUT);
}
boolean st=true;
/*---------------------Main Loop--------------------------------------------------------------*/
void loop(){
//  
//  delay(6000);
 //forward(14);
// delay(2000000000);

 char* temp= getRPiMsg();
  if(strlen(temp)<=0)
  {
    return;
  }
 else if (strlen(temp) == 1) {
    make_move(temp[0]);
    memset(temp, 0, sizeof(temp));
  } 
  else {
   int n = strlen(temp);
    for (int i = 0; i < n; i++){
      make_move(temp[i]);
      delay(100);
      
    }
    memset(temp, 0, sizeof(temp));
  }
}


/*------------------ RPI Functions ---------------------*/
//Serial Read String Variable
char command[64]; // Allocate some space for the string
//int index = 0; // Index into array; where to store the character

char* getRPiMsg() {
  memset(command,0,sizeof(command));
  Serial.readBytes(command,64);
  return command;
  
}

void setRPiMsg(char message){
  Serial.print("BP<");
  Serial.print(message);
  Serial.print('\0');
  Serial.print('\n');
  Serial.flush();
}

void setRPiMsg(int l, int fl,int mid, int fr, int rf, int rb){

  Serial.print("BP<");
  Serial.print(l);
  Serial.print(":");
  Serial.print(fl);
  Serial.print(":");
  Serial.print(mid);
  Serial.print(":");                
  Serial.print(fr);
  Serial.print(":");
  Serial.print(rf);
  Serial.print(":");
  Serial.print(rb);
  Serial.print('\0');
  Serial.print('\n');
  Serial.flush();
}


/*-------------------------------------Interrupt Handling------------------------------------------*/
void MotorLeftA(){
  motorL_newB ^ motorL_oldA ? encoderLeft++: encoderLeft--;
  motorL_oldA = digitalRead(motorL_A);

}

void MotorLeftB(){
  motorL_newB=digitalRead(motorL_B);
  motorL_newB ^ motorL_oldA ? encoderLeft++ : encoderLeft--;
}
void MotorRightA(){
  motorR_newB ^ motorR_oldA ? encoderRight--: encoderRight++;
  motorR_oldA = digitalRead(motorR_A);

}

void MotorRightB(){
  motorR_newB=digitalRead(motorR_B);
  motorR_newB ^ motorR_oldA ? encoderRight-- : encoderRight++;
}


/*--------------------------------Speeds-----------------------------------------------------*/

void currentSpeed(){
  double currentLEncoder=encoderLeft;
  double currentREncoder=encoderRight;
 /* Serial.print("Left Side Motor Ticks: ");
  Serial.println(currentLEncoder);
  Serial.print("Right Side Motor Ticks: ");
  Serial.println(currentREncoder);*/
  delay(5);
  currentTime=millis();
  double time=currentTime-lastTime;
  leftS=(currentLEncoder-lastCountLeft)*60.0*1000.0/(2249*time);
  rightS=(currentREncoder-lastCountRight)*60.0*1000.0/(2249*time);
  lastCountLeft=currentLEncoder;
  lastCountRight=currentREncoder;
  lastTime=currentTime;
}

/*-------------------------------current distance--------------------------------------------*/
void currentDistanceMoved(){
  double currentLEncoder=encoderLeft;
  double dEncoder, deltaDistance;
  dEncoder=currentLEncoder-prevTicks;
//  Serial.print("Right (Now-Before): ");
//  Serial.println(currentLEncoder);
  deltaDistance=dEncoder*8381305.434;
  //Serial.print("undivided: ");
  //Serial.println(deltaDistance);
  deltaDistance/=1000000000;  
  distanceMoved+=deltaDistance;
  prevTicks=currentLEncoder;
}

void currentDistanceMovedR(){
  double currentREncoder=encoderRight;
  double dEncoder, deltaDistance;
  dEncoder=currentREncoder-prevTicksR;
//  Serial.print("Right (Now-Before): ");
//  Serial.println(currentLEncoder);
  deltaDistance=dEncoder*8381305.434;
  //Serial.print("undivided: ");
  //Serial.println(deltaDistance);
  deltaDistance/=1000000000;  
  distanceMoved+=deltaDistance;
  prevTicksR=currentREncoder;
}

/*---------------PID FORWARD----------------------*/
void motorPIDF(){
  
  currentSpeed();
//  Serial.print("Right (Before): ");
//   Serial.println(leftS);
//   Serial.print("Left Power (Before): ");
//   Serial.println(rightS);
  error=abs(leftS)-abs(rightS);
//  Serial.print("Error: ");
 // Serial.println(error);
  dError=error-oldError;
  oldError=error;
  sError+=error;
  // Kfp=3.5;  //2.75  //Goes more right-increase. 
  double Kd=2.75;  //0.06
  double Ki=0.05;
  if(sError>255)
    sError=255;
  outputF=error*Kfp-dError*Kd+Ki*sError;

}

void motorPIDFS(){
  
  currentSpeed();
//  Serial.print("Right (Before): ");
//   Serial.println(leftS);
//   Serial.print("Left Power (Before): ");
//   Serial.println(rightS);
  error=abs(leftS)-abs(rightS);
//  Serial.print("Error: ");
 // Serial.println(error);
  dError=error-oldError;
  oldError=error;
  sError+=error;
  double Kp=3.6;  //3.8  Goes more right-decrease
  double Kd=2.8;  //0.06
  double Ki=0.05;
  if(sError>255)
    sError=255;
  outputF=error*Kp-dError*Kd+Ki*sError;

}

double motorPIDR(int Left, int Right){
  double output;
  error=Left+Right;
//  Serial.print("Error:");
//  Serial.println(error);
  dError=error-oldError;
  oldError=error;
  sError+=error;
  float Kp=0.02;  
  float Kd=0.06;  
  float Ki=0;
  if(sError>255)
    sError=255;

  output=error*Kp+dError*Kd+Ki*sError;
//    Serial.print("output");
//  Serial.println(output);
  return output;

}

double motorPIDL(int Left, int Right){
  double output;
  error=Left+Right;
  dError=error-oldError;
  oldError=error;
  sError+=error;
  float Kp=1.05;  //2.75
  float Kd=0.03;  //0.06
  float Ki=0;
  if(sError>255)
    sError=255;
  output=error*Kp+dError*Kd+Ki*sError;
 // Serial.print("Error:");
  //Serial.println(error);
   // Serial.print("Output:");
  //Serial.println(output);
  return output;

}

void resetPID(){
  oldError=0;
  prevTicks=0;
  prevTicksR=0;
  encoderRight=encoderLeft=0;
  // lt=0;
}


/*---------------------Motion(forward, rotation)---------------------------------*/

/*---------------------------------Movements----------------------------------*/

/*-----------------------EXPLOARATION PATH FUNC---------------*/
void forwardShort(int block){
  double output;
  encoderLeft=encoderRight=0;
  prevTicks=0;
  int offset=0;
  leftPWM= 90;
  rightPWM = 90;
  int LeftPosition, RightPosition;
int multiplier;
  switch(block){

    case 1: multiplier = 1100; break;
    case 2: multiplier = 1145; break;
    case 3: multiplier = 1157; break;
    case 4: multiplier = 1170; break;
    case 5: multiplier=1170; break;
    case 10: multiplier = 1180; break;
    case 11: multiplier = 1182; break;
    case 12: multiplier = 1182; break;
    
    default: multiplier = 1180; break;
  
  }
  int targetDistance = multiplier * block;
  
  distanceMoved=0;
 while(1){
    LeftPosition = encoderLeft;    
    RightPosition = encoderRight;  

 
    
    if(LeftPosition >= targetDistance){
      md.setM2Brake(400);
      delay(1);
      md.setM1Brake(400);
//      delay(100);
//      md.setBrakes(0, 0);
//    Serial.print("Left Position:");
//     Serial.println(LeftPosition);
   /*   Serial.print("TD:");
     Serial.println(targetDistance);*/
      break;
    }
   motorPIDFS();

//Serial.print("Left:");
//  Serial.println(leftPWM-output-offset);
//  Serial.print("Right:");
//  Serial.println(rightPWM+output);
  md.setSpeeds(-(leftPWM-outputF), -(rightPWM+outputF));
    
}
}

void rotateLeft(int degree){  
  leftPWM=120;
  rightPWM=120;  
  distanceMoved=0.0;
  resetPID();
  double currentREncoder,currentLEncoder,output;
  float targetAngle;
  targetAngle=1.575*8.15;              //default 90 degree
  if(degree==90)
  targetAngle=1.575*8.15;              //R=8.2 assuming
  if(degree==180)
  targetAngle=3.15*8.35;              //
  md.setSpeeds(leftPWM,-rightPWM);
  while(1){ 
    cT=millis();
   if(cT-lT>50)
 {
      lT=cT;
     currentREncoder=encoderRight;
      currentLEncoder=encoderLeft;
//      //  Serial.print("Left Side Motor Ticks: ");
//      // Serial.println(currentLEncoder);
//      //Serial.print("Right Side Motor Ticks: ");
//      //Serial.println(currentREncoder); 
      output=motorPIDL(currentLEncoder, currentREncoder);
//      //Serial.print("Output: ");
//      //Serial.println(output); 
//      //Serial.print("L: ");
//      //Serial.println(leftPWM+output); 
//      //Serial.print("R: ");
//      //Serial.println(-(rightPWM-output)); 
//      // leftPWM+=output;
//      // rightPWM-=output;
      md.setSpeeds(leftPWM+output,-(rightPWM-output));
    }
    currentDistanceMoved();
    //Serial.print("\nDISTANCE: ");
    //Serial.print(distanceMoved);
    //Serial.println("cm");
    //Serial.print("\nTARGET ANGLE DISTANCE (RADIANS): ");
    //Serial.print(targetAngle);
    //Serial.println("cm");
    if(abs(distanceMoved)>=abs(targetAngle)-0.32)      // -1.9 for 90 because of the rate at which distance is being calculated.
    {
     // Serial.print("STOPPED: ");
      // Serial.println("cm");
      md.setBrakes(400,400);
//       delay(100);
//      md.setBrakes(0, 0);
      // prevRotErrorL=abs(distanceMoved)-abs(targetAngle);
      //Serial.println(prevRotErrorL);

      break;
    }
  }  
}

void rotateRight(int degree){          //+ve degree for left rotation, -ve degree for right rotation
  distanceMoved=0;
  double output;
  leftPWM=120;
  rightPWM=120;
  resetPID();
  float targetAngle;
  double encoR,encoL;
  targetAngle=1.575*8.15;                //R=8.2 assuming

  md.setSpeeds(-leftPWM,rightPWM);
  while(1){ 
    cT=millis();
    if(cT-lT>50)
    {
      lT=cT;
      encoR=encoderRight;
      encoL=encoderLeft;
      output=motorPIDR(encoL,encoR);
//        Serial.print("Left:");
//        Serial.println(leftPWM-output);
//        Serial.print("Right:");
//        Serial.println(rightPWM+output);
      md.setSpeeds(-(leftPWM+output),rightPWM-output);
    }
    currentDistanceMovedR();
    // Serial.print("\nDISTANCE: ");
    // Serial.print(distanceMoved);
    // Serial.println("cm");
    // Serial.print("\nTARGET ANGLE DISTANCE (RADIANS): ");
    // Serial.print(targetAngle);
    // Serial.println("cm");
    if(abs(distanceMoved)>=abs(targetAngle)-0.3)      // -1.9 for 90 because of the rate at which distance is being calculated.
    {
//      Serial.print("\nDISTANCE: ");
//      Serial.print(distanceMoved);
//      Serial.println("cm");
/* md.setM2Brake(400);
  delay(100);
  md.setM1Brake(400);*/
 md.setBrakes(400,400);
      //delay(100);
      //md.setBrakes(0, 0);

      break;
    }
  }  
}







/*------------FASTEST PATH-----------------*/
void forward(int block){
  double output;
  encoderLeft=encoderRight=0;
  prevTicks=0;
  int offset=0;
  leftPWM= 175;
  rightPWM = 175;
  double k=0.35;
  int LeftPosition, RightPosition;
int multiplier;

  switch(block){

    case 1: 
    multiplier = 1030;
    Kfp=3.6;
    break;
    case 2: multiplier = 1100;
     Kfp=3.8;
     break;
    case 3: multiplier = 1100;
     Kfp=3.8;
     break;
    case 4: multiplier = 1130;
    Kfp=3.7;
   break;
    case 5: multiplier=1140;
     Kfp=3.55;
   break;
    case 6: multiplier=1150;
     Kfp=3.1;
   break;
    case 7: multiplier=1155;
     Kfp=3.1;
   break;
    case 8: multiplier=1160;
     Kfp=2.85;
   break;
    case 9: multiplier=1160;
     Kfp=2.7;
   break;
    case 10: multiplier = 1160;
     Kfp=2.45;
   break;
    case 11: multiplier = 1160;
     Kfp=2.25;
   break;
    case 12: multiplier = 1160;
     Kfp=2;
   break;
    case 13: multiplier = 1160;
     Kfp=3.75;
   break;
    case 14: multiplier = 1160;
     Kfp=3.6;
   break;
    case 15: multiplier = 1160;
     Kfp=3.35;
   break;
    default:
    multiplier = 1160;
     Kfp=3.5;
 
  
  }
  Kfp-=k;
  int targetDistance = (multiplier+offset) * block;
  
  distanceMoved=0;
 while(1){
    LeftPosition = encoderLeft;    
    RightPosition = encoderRight;  

 
    
    if(LeftPosition >= targetDistance){
      md.setM2Brake(400);
      delay(1);
      md.setM1Brake(400);
      delay(500);
//      delay(100);
//      md.setBrakes(0, 0);
//    Serial.print("Left Position:");
//     Serial.println(LeftPosition);
   /*   Serial.print("TD:");
     Serial.println(targetDistance);*/
      break;
    }
   motorPIDF();

//Serial.print("Left:");
//  Serial.println(leftPWM-output-offset);
//  Serial.print("Right:");
//  Serial.println(rightPWM+output);
  md.setSpeeds(-(leftPWM-outputF), -(rightPWM+outputF));
    
}
}

void rotateRightFP(int degree){          //+ve degree for left rotation, -ve degree for right rotation
  distanceMoved=0;
  double output;
  leftPWM=175;
  rightPWM=175;
  resetPID();
  float targetAngle;
  double encoR,encoL;
  targetAngle=1.575*7.8;                //R=8.2 assuming

  md.setSpeeds(-leftPWM,rightPWM);
  while(1){ 
    cT=millis();
    if(cT-lT>50)
    {
      lT=cT;
      encoR=encoderRight;
      encoL=encoderLeft;
      output=motorPIDR(encoL,encoR);
//        Serial.print("Left:");
//        Serial.println(leftPWM-output);
//        Serial.print("Right:");
//        Serial.println(rightPWM+output);
      md.setSpeeds(-(leftPWM+output),rightPWM-output);
    }
    currentDistanceMovedR();
    // Serial.print("\nDISTANCE: ");
    // Serial.print(distanceMoved);
    // Serial.println("cm");
    // Serial.print("\nTARGET ANGLE DISTANCE (RADIANS): ");
    // Serial.print(targetAngle);
    // Serial.println("cm");
    if(abs(distanceMoved)>=abs(targetAngle)-0.3)      // -1.9 for 90 because of the rate at which distance is being calculated.
    {
//      Serial.print("\nDISTANCE: ");
//      Serial.print(distanceMoved);
//      Serial.println("cm");
/* md.setM2Brake(400);
  delay(100);
  md.setM1Brake(400);*/
 md.setBrakes(400,400);
      //delay(100);
      //md.setBrakes(0, 0);

      break;
    }
  }  
}

void rotateLeftFP(int degree){  
  leftPWM=175;
  rightPWM=175;  
  distanceMoved=0.0;
  resetPID();
  double currentREncoder,currentLEncoder,output;
  float targetAngle;
  targetAngle=1.575*7.8;              //default 90 degree
//  if(degree==90)
//  targetAngle=1.575*7.9;              //R=8.2 assuming
//  if(degree==180)
//  targetAngle=3.15*8.15;              //
  md.setSpeeds(leftPWM,-rightPWM);
  while(1){ 
    cT=millis();
   if(cT-lT>50)
 {
      lT=cT;
     currentREncoder=encoderRight;
      currentLEncoder=encoderLeft;
//      //  Serial.print("Left Side Motor Ticks: ");
//      // Serial.println(currentLEncoder);
//      //Serial.print("Right Side Motor Ticks: ");
//      //Serial.println(currentREncoder); 
      output=motorPIDL(currentLEncoder, currentREncoder);
//      //Serial.print("Output: ");
//      //Serial.println(output); 
//      //Serial.print("L: ");
//      //Serial.println(leftPWM+output); 
//      //Serial.print("R: ");
//      //Serial.println(-(rightPWM-output)); 
//      // leftPWM+=output;
//      // rightPWM-=output;
      md.setSpeeds(leftPWM+output,-(rightPWM-output));
    }
    currentDistanceMoved();
    //Serial.print("\nDISTANCE: ");
    //Serial.print(distanceMoved);
    //Serial.println("cm");
    //Serial.print("\nTARGET ANGLE DISTANCE (RADIANS): ");
    //Serial.print(targetAngle);
    //Serial.println("cm");
    if(abs(distanceMoved)>=abs(targetAngle)-0.32)      // -1.9 for 90 because of the rate at which distance is being calculated.
    {
     // Serial.print("STOPPED: ");
      // Serial.println("cm");
      md.setBrakes(400,400);
//       delay(100);
//      md.setBrakes(0, 0);
      // prevRotErrorL=abs(distanceMoved)-abs(targetAngle);
      //Serial.println(prevRotErrorL);

      break;
    }
  }  
}


void rightDrift(){
md.setSpeeds(-400,0);
delay(750);
md.setBrakes(400,400);
delay(100);
}



boolean test=true;

/*--------------------Dummy movements----------------------------------------------------------*/
void make_move(char c)
{
  switch(c){
  case 'X' : 
    forwardShort(1);
    doScan();
    //setRPiMsg(c);
    break;

  case 'L' : 
    rotateLeft(90);
    delay(250);
    doScan();
  //  delay(200);  
   // setRPiMsg(c);
    break;

  case 'R' : 
    rotateRight(90);
    delay(250);
    doScan();
  //  delay(200);
   // setRPiMsg(c);
    break;
    
  case 'P' : 
    rotateLeft(180);
    delay(250);
    doScan();
  //  delay(200);
   // setRPiMsg(c);
    break;
    
  case 'Q' : 
    startSequence();
    setRPiMsg('Q');
    break;
    
  case 'W' : 
    startSequence2();
    setRPiMsg('W');
    break;

  case 'S' : 
    doScan();
    break;

  case 'T' :
    FCalibrate();
    setRPiMsg('T');
    break;

  case 'Y' :
    RCalibrate();
    setRPiMsg('Y');
    break;
    
  case 'U' :
    LCalibrate();
    setRPiMsg('U');
    break;    
   
  case '1' : 
    forward(1);
    break;
    
   case '2' : 
    forward(2);
    break;
        
   case '3' : 
    forward(3);
    break;
    
   case '4' : 
    forward(4);
    break;
    
    
   case '5' : 
    forward(5);
    //doScan();
    break;
    
   case '6' : 
    forward(6);
    //doScan();
    break;
    
  case '7' : 
    forward(7);
    //doScan();
    break;
    
   case '8' : 
    forward(8);
    //doScan();
    break;
    
  case '9' : 
    forward(9);
    //doScan();
    break;
    
  case 'A' : 
    forward(10);
    //doScan();
    break; 
    
  case 'B' : 
    forward(10);
   // doScan();
    break;
    
  case 'C' : 
    forward(11);
    //doScan();
    break;
    
  case 'D' : 
    forward(12);
   // doScan();
    break;
    
  case 'E' : 
    forward(13);
   // doScan();
    break;
    
  case 'F' : 
    forward(15);
   // doScan();
    break;
    
  case 'G' : 
    rotateRightFP(90);
        delay(500);
    //doScan();
    break;
    
  case 'H' : 
    rotateLeftFP(90);
        delay(500);
   // doScan();
    break;
    
  case 'I' : 
    midDist();
    setRPiMsg('i');
    break;
    
   
  default : 
    halt();
  }  
}


void doScan() {
  delay(300);
  //Reading Sensor Value
  double val0 = 0;
  double val1 = 0;
  double val2 = 0;
  double val3 = 0;
  double val4 = 0;
  double val5 = 0;
 
  val0 = calcDist(irL);
  val1 = calcDist(irFL);
  val2 = calcDist(irFR);
  val3 = calcDist(irRF);
  val4 = calcDist(irRB);
  val5 = calcDist(irMid);
  

/*
   Serial.print("L: ");
   Serial.print(val0);
//   Serial.print(" Block: ");
// //  Serial.println(sensorLS);
   Serial.print("FL: ");
   Serial.print(val1);
//   Serial.print(" Block: ");
//  // Serial.println(sensorFLS);
   Serial.print("M: ");
   Serial.print(val5);
//   Serial.print(" Block: ");
//   //Serial.println(sensorMidS);
   Serial.print("FR: ");
   Serial.print(val2);
//   Serial.print(" Block: ");
//  // Serial.println(sensorFRS);
   Serial.print("RF: ");
   Serial.print(val3);
//   Serial.print(" Block: ");
//   //Serial.println(sensorRFS);
   Serial.print("RB: ");
   Serial.print(val4);
//   Serial.print(" Block: ");
//   //Serial.println(sensorRBS);
   Serial.println();
*/
  //setRPiMsg(sensorLS, sensorFLS,sensorMidS, sensorFRS, sensorRFS, sensorRBS);
  
  
  setRPiMsg(val0, val1,val5, val2, val3, val4);
}


double calcDist(int pin){
  double rawRead,dist=-1;
  rawRead=averageFeedback(pin);
switch(pin)
{
case irL:
if(rawRead>200)
dist=60992*pow(rawRead,-1.297);
break;
case irFL:
if(rawRead>100)
dist=17631*pow(rawRead,-1.244);
break;
case irFR:
if(rawRead>140)
dist=16641*pow(rawRead,-1.222);
break;
case irMid:
if(rawRead>200)
dist=263472*pow(rawRead,-1.548);
break;
case irRB:
if(rawRead>107)
dist=16259*pow(rawRead,-1.234);
break;
case irRF:
if(rawRead>100)
dist=22323*pow(rawRead,-1.281);
break;
}
return dist;

}


void insertionsort(double array[], int length){
  int i,j;
  double temp;
  for(i = 1; i < length; i++){
    for(j = i; j > 0; j--){
      if(array[j] < array[j-1]){
        temp = array[j];
        array[j] = array[j-1];
        array[j-1] = temp;
      }
      else
        break;
    }
  }
}


double averageFeedback(int pin){
  double x[50];
  int i;

  for(i=0;i<50;i++){
    x[i] =analogRead(pin);
  }
  insertionsort(x, 50);
  return calcFreq(x,50);
}

double calcFreq(double arr[], int length){
  double value;
  int count, countMax=0;
  int i=0,j=0,pos=0;
  while(i<length){
    count=0;
    for(j=i; j<length; j++){
    if(arr[i]==arr[j]) count++;
    else{
    i=j;
    break;
    }
    }
    if(count>countMax){
    value=arr[j-1];
    countMax=count;
    }
    else if(count==countMax){
    value=value>arr[j-1]?value:arr[j-1];
    }
    if(j==length)
    break;
  }
  
  return value;
}



/*--------------------OOO----------------------*/
void halt(){
  md.setSpeeds(0,0); //no movements
}


void RCalibrate()
{
  //parallel to the wall
  double s3 = calcDist(irRF);
 // double s4= calcDist(irRB);
  int count = 0;
  
 /*  if(s4<5.5){
    rotateLeft(90);
    md.setSpeeds(-51,50);
    delay(200);
    md.setBrakes(400,400);
    delay(100);
    rotateRight(90);
    }*/
    
    if(s3>= 7.3 || s3<6.5)
  {
    rotateRight(90);
    delay(100);
    FCalibrate();
    delay(100);
    rotateLeft(90);
    delay(100);
  }
  s3 = calcDist(irRF);
  count = 0;
  
}

void FCalibratedist()
{
  double s1=0;
int count=0;
while(count<10)
{
  s1 = calcDist(irFL);
  if(s1<=7)
  {
    md.setSpeeds(51,50);
    delay(100);
    md.setBrakes(400,400);
   
  }
  else if(s1>=7.4)
  {
    md.setSpeeds(-50,-50);
    delay(100);
    md.setBrakes(400,400);
  }
  
  else
  break;
  count++;
}

}





void FCalibrate()
{
double s3=calcDist(irMid);
int count=0;
while(s3<=15.5 &&  count<5)
{
   md.setSpeeds(51,50);
   delay(100);
   md.setBrakes(400,400);
   s3=calcDist(irMid);
   count++;
}
calF(); 
FCalibratedist();
calF(); 
}


void LCalibrate()
{
  double sL = calcDist(irL);
  //Serial.println(sL);
  if(sL < 17.5 || sL > 17.8)
  {
    rotateLeft(90);
    delay(300);
    FCalibrate();
    delay(300);
    rotateRight(90);
    delay(200);
  }
}


void startSequence()
{
  FCalibrate();
  delay(300);   
  rotateRight(90);
  delay(300);
  FCalibrate();
  delay(300);
  rotateRight(90);
  delay(200);
}

void startSequence2()
{
  FCalibrate();
  delay(300);   
  rotateLeft(90);
  delay(300);
  FCalibrate();
  delay(300);;
  rotateLeft(90);
  delay(200);
}


double getRaw(int pin)
{
   double raw = 0;
   
   for(int i = 0; i<5; i++)
   {
     raw +=analogRead(pin);
   }
   raw = raw/5;
   
   return raw;
}


void calF()
{
  int count=0;
   double s1 = calcDist(irFL);
 double s2 = calcDist(irFR);
   while(((s1 <= (s2-0.3))|| (s1>=s2+0.3)) && count < 8  )
  {
    if(s1 > s2+0.1)
    {
      md.setSpeeds(-41,40);
      delay(100);
      md.setBrakes(400,400);
      delay(100);
    }
    else if(s1 < s2)
    {
      md.setSpeeds(41,-40);
      delay(105);
      md.setBrakes(400,400);
      delay(100);
    }
     s1 = calcDist(irFL);
     s2 = calcDist(irFR);
     count++;
  }
  
  /*
  double fL = getRaw(irFL);
  double fR = getRaw(irFR)-35;  
  double difference = abs(fL - fR);
  int count = 0;
  
  while(difference > 5.0 && count <10 )
  {
    if(fL > fR)
    {
      md.setSpeeds(50,-50);
      delay(100);
      md.setBrakes(400,400);
      delay(100);
    }
    
    else if(fR > fL)
    {
      md.setSpeeds(-50,50);
      delay(100);
      md.setBrakes(400,400);
      delay(100);
    }
    delay(50);
    count++;
    fL = getRaw(irFL);
    fR = getRaw(irFR)-35;
    difference = abs(fL - fR);
   
  }*/
    
}

void midDist(){
double s1=calcDist(irMid);
double s2=calcDist(irFL);
double s3=calcDist(irFR);

//int count=0;
while(s1>17 ){
  if(s3<5 || s2<5)
     break;

    md.setSpeeds(-150,-150);
    delay(200);
    md.setBrakes(400,400);
    delay(500);
  //count++;
  s1 = calcDist(irMid);
  s2=calcDist(irFL);
  s3=calcDist(irFR);
}

}

