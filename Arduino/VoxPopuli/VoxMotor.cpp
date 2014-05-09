#include "Arduino.h"
#include "VoxMotor.h"

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

  currentState = VoxMotor::WAIT;
  currentPosition = targetPosition = 0;
}

void VoxMotor::stop() {
  analogWrite(pin[currentDirection], 255);
}

boolean VoxMotor::isDone() {
  return (currentState == DONE);
}
void VoxMotor::goWait() {
  currentState = WAIT;
}

void VoxMotor::setTarget(byte t) {
  targetPosition = t;
  currentDutyCycle = 0.0;
  analogWrite(pin[0], 255);
  analogWrite(pin[1], 255);

  if(abs(targetPosition - currentPosition) > 100){
    currentDirection = (random(10)<5);
    rampDurationMillis = random(500,800);
    changeStateMillis = millis()+rampDurationMillis;
    currentState = SPEED_UP;
  }
  else{
    currentState = DONE;
  }
}

void VoxMotor::update() {
  // deal with direction
  if((digitalRead(limit[0]) == LOW) || (digitalRead(limit[1]) == LOW)){
    currentDirection = (currentDirection+1)%2;
  }

  if((currentState == DONE) || (currentState == WAIT)){
    currentDutyCycle = 0.0;
    analogWrite(pin[0], 255);
    analogWrite(pin[1], 255);
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
      currentState = DONE;
      currentPosition = targetPosition;
    }
  }
}

