#include "VoxMotor.h"

#define PAN_PWM0 2
#define PAN_PWM1 3
#define PAN_SWITCH0 A10
#define PAN_SWITCH1 A11

#define TILT_PWM0 4
#define TILT_PWM1 5
#define TILT_SWITCH0 A8
#define TILT_SWITCH1 A9

VoxMotor panMotor(PAN_PWM0, PAN_PWM1, PAN_SWITCH0, PAN_SWITCH1);
VoxMotor tiltMotor(TILT_PWM0, TILT_PWM1, TILT_SWITCH0, TILT_SWITCH1);

void setup() {
  Serial.begin(57600);
  Serial1.begin(9600);
  pinMode(13,OUTPUT);
  digitalWrite(13,LOW);

  Serial.print("\r\nVox Populi Started");
  if(DEBUG){
    panMotor.setTarget(255);
    tiltMotor.setTarget(255);
  }
}

void loop() {
  if(Serial1.available() > 3){
    int header[2];
    header[0] = Serial1.read() & 0xff;
    header[1] = Serial1.read() & 0xff;
    if((header[0] == 'F') && (header[1] == 'Q')){
      panMotor.setTarget(Serial1.read());
      tiltMotor.setTarget(Serial1.read());
    }
    else if((header[0] == 'L') && (header[1] == 'E')){
      Serial1.read();
      digitalWrite(13, Serial1.read() ? HIGH : LOW);
    }
    else{
      Serial.print("got: ");
      Serial.print(header[0]);
      Serial.print(" and ");
      Serial.println(header[1]);
    }
    while(Serial1.available()) Serial1.read();
  }

  panMotor.update();
  tiltMotor.update();

  if(panMotor.isDone() && tiltMotor.isDone()){
    Serial1.write('G');
    Serial1.write('O');
    Serial1.flush();
    panMotor.goWait();
    tiltMotor.goWait();
    if(DEBUG){
      panMotor.setTarget(255);
      tiltMotor.setTarget(255);
    }
  }
}

