#define EN_IN A0
#define DIR_IN A1
#define PUL_IN A2

#define EN_OUT 5
#define DIR_OUT 6
#define PUL_OUT 7

void setup() {
    pinMode(EN_IN, INPUT);
    pinMode(DIR_IN, INPUT);
    pinMode(PUL_IN, INPUT);

    pinMode(EN_OUT, OUTPUT);
    pinMode(DIR_OUT, OUTPUT);
    pinMode(PUL_OUT, OUTPUT);
}

void loop() {
  digitalWrite(EN_OUT, (analogRead(EN_IN) > 500) ? HIGH : LOW);
  digitalWrite(DIR_OUT, (analogRead(DIR_IN) > 500) ? HIGH : LOW);
  digitalWrite(PUL_OUT, (analogRead(PUL_IN) > 500) ? HIGH : LOW);
}
