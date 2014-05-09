#pragma once

class VoxMotor {
  enum StateType { 
    WAIT, SPEED_UP, SPEED_DOWN, DONE };
  int pin[2];
  int limit[2];
  short currentDirection;
  float currentDutyCycle;
  unsigned long changeStateMillis;
  unsigned long rampDurationMillis;
  StateType currentState;
  byte targetPosition, currentPosition;

public:
  VoxMotor(int motor0, int motor1, int switch0, int switch1);
  void stop();
  void update();
  boolean isDone();
  void goWait();
  void setTarget(byte t);
};

