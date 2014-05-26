#pragma once
#include "Arduino.h"

#define DEBUG 1

class VoxMotor {
  enum StateType { 
    WAIT, MOVE_FORWARD, MOVE_BACK, DONE };
  int pin[2];
  int limit[2];
  short currentDirection;
  float currentDutyCycle;
  unsigned long moveDurationMillis;
  unsigned long moveStartMillis;
  StateType currentState;

public:
  VoxMotor(int motor0, int motor1, int switch0, int switch1);
  void stopAndChangeDirection();
  void update();
  boolean isDone();
  void goWait();
  void setTarget(uint8_t t);
};

