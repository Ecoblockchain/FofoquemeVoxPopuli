#define PAN_PWM0 2
#define PAN_PWM1 3
#define TILT_PWM0 4
#define TILT_PWM1 5

void setup() {
  Serial.begin(9600);
  pinMode(13,OUTPUT);
  digitalWrite(13,LOW);

  pinMode(PAN_PWM0, OUTPUT);
  pinMode(PAN_PWM1, OUTPUT);
  digitalWrite(PAN_PWM0,0);
  digitalWrite(PAN_PWM1,0);

  pinMode(TILT_PWM0, OUTPUT);
  pinMode(TILT_PWM1, OUTPUT);
  digitalWrite(TILT_PWM0,0);
  digitalWrite(TILT_PWM1,0);
}

void loop() {
  if(Serial.available() > 3){
    int header[2];
    header[0] = Serial.read() & 0xff;
    header[1] = Serial.read() & 0xff;
    if((header[0] == 'F') && (header[1] == 'Q')){
      digitalWrite(PAN_PWM0, Serial.read());
      digitalWrite(TILT_PWM0, Serial.read());
    }
    else if((header[0] == 'L') && (header[1] == 'E')){
      Serial.read();
      digitalWrite(13, Serial.read() ? HIGH : LOW);
    }
    while(Serial.available()) Serial.read();
  }
}

