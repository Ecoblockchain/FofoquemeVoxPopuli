#include "Arduino.h"
#include "VoxMotor.h"

#define PWM_CALIBRATE_DUTY 0.5
#define PWM_MAX_DUTY 0.6


VoxMotor::VoxMotor(){
  pin[0] = pin[1] = 0;
  limit[0] = limit[1] = 0;
  currentDirection = 0;
  currentDutyCycle = 0.0;
}

void VoxMotor::setup(int motor0, int motor1, int switch0, int switch1){
  // assume pwm'ing pin[0] will trigger limit[0]
  pin[0] = motor0;
  pin[1] = motor1;
  limit[0] = switch0;
  limit[1] = switch1;

  pinMode(pin[0], OUTPUT);
  pinMode(pin[1], OUTPUT);
  pinMode(limit[0], INPUT_PULLUP);
  pinMode(limit[1], INPUT_PULLUP);

  // make sure switches are not pressed initially
  if(!(digitalRead(limit[0]) && digitalRead(limit[1]))){
    analogWrite(pin[0], 255);
    analogWrite(pin[1], (1.0-PWM_CALIBRATE_DUTY)*255.0);
    delay(300);
    analogWrite(pin[0], 255);
    analogWrite(pin[1], 255);
    delay(500);
  }

  if(!(digitalRead(limit[0]) && digitalRead(limit[1]))){
    analogWrite(pin[0], (1.0-PWM_CALIBRATE_DUTY)*255.0);
    analogWrite(pin[1], 255);
    delay(300);
    analogWrite(pin[0], 255);
    analogWrite(pin[1], 255);
    delay(500);
  }

  boolean calibrate = true;
  // pwm motor0
  analogWrite(pin[0], (1.0-PWM_CALIBRATE_DUTY)*255.0);
  analogWrite(pin[1], 255);

  while(calibrate){
    // if limit[0] was hit, stop calibration
    if(!digitalRead(limit[0])){
      calibrate = false;
      analogWrite(pin[0], 255);
    }

    // if limit[1] was hit, swicth limits and stop calibration
    if(!digitalRead(limit[1])){
      limit[0] = switch1;
      limit[1] = switch0;
      calibrate = false;
      analogWrite(pin[0], 255);
    }
  }

  // unpress switch
  while(!(digitalRead(limit[0]) && digitalRead(limit[1]))){
    analogWrite(pin[0], 255);
    analogWrite(pin[1], (1.0-PWM_CALIBRATE_DUTY)*255.0);
  }
  analogWrite(pin[1], 255);

  changeStateMillis = millis() + 1000;
  currentState = VoxMotor::PAUSE;
}

void VoxMotor::stop() {
  analogWrite(pin[currentDirection], 255);
}

void VoxMotor::update() {
  // deal with direction
  if(digitalRead(limit[0]) == LOW){
    currentDirection = 1;
  }
  if(digitalRead(limit[1]) == LOW){
    currentDirection = 0;
  }

  if(currentState == PAUSE){
    currentDutyCycle = 0.0;
    analogWrite(pin[0], 255);
    analogWrite(pin[1], 255);
    if(millis() > changeStateMillis){
      currentDirection = (random(10)<5);
      rampDurationMillis = random(500,800);
      changeStateMillis = millis()+rampDurationMillis;
        currentState = SPEED_UP;
    }
  }
  if(currentState == SPEED_UP){
    currentDutyCycle += (currentDutyCycle<PWM_MAX_DUTY)?0.1:0;
    analogWrite(pin[0], (currentDirection==0)?(1.0-currentDutyCycle)*255.0:255);
    analogWrite(pin[1], (currentDirection==1)?(1.0-currentDutyCycle)*255.0:255);
    if(millis() > changeStateMillis){
      changeStateMillis = millis()+rampDurationMillis;
      currentState = SPEED_DOWN;
    }
  }
  if(currentState == SPEED_DOWN){
    currentDutyCycle -= (currentDutyCycle>0.1)?0.1:0;
    analogWrite(pin[0], (currentDirection==0)?(1.0-currentDutyCycle)*255.0:255);
    analogWrite(pin[1], (currentDirection==1)?(1.0-currentDutyCycle)*255.0:255);
    if(millis() > changeStateMillis){
      changeStateMillis = millis()+random(2000,4000);
      currentState = PAUSE;
    }
  }
}

