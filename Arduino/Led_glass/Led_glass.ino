
#include <EEPROM.h>
#include <Wire.h>
#include "I2Cdev.h"
#include "MPU6050_6Axis_MotionApps20.h"
#include "Wire.h"



#include <Adafruit_NeoPixel.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
#include <avr/power.h>
#endif

#define PIN 12
#define NUM_LEDS 20
#define BRIGHTNESS 50
Adafruit_NeoPixel strip = Adafruit_NeoPixel(NUM_LEDS, PIN, NEO_RGB + NEO_KHZ800);



#define POWER_ON 9
#define CMD 7
#define INTERRUPT_PIN 2
#define LED_PIN 13
#define CANPAI_THRESHOLD 45000000
#define CANPAI_WAITTIME 500
#define Array 100

byte neopix_gamma[] = {
  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  1,  1,  1,
  1,  1,  1,  1,  1,  1,  1,  1,  1,  2,  2,  2,  2,  2,  2,  2,
  2,  3,  3,  3,  3,  3,  3,  3,  4,  4,  4,  4,  4,  5,  5,  5,
  5,  6,  6,  6,  6,  7,  7,  7,  7,  8,  8,  8,  9,  9,  9, 10,
  10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 15, 15, 16, 16,
  17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 24, 24, 25,
  25, 26, 27, 27, 28, 29, 29, 30, 31, 32, 32, 33, 34, 35, 35, 36,
  37, 38, 39, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 50,
  51, 52, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 66, 67, 68,
  69, 70, 72, 73, 74, 75, 77, 78, 79, 81, 82, 83, 85, 86, 87, 89,
  90, 92, 93, 95, 96, 98, 99, 101, 102, 104, 105, 107, 109, 110, 112, 114,
  115, 117, 119, 120, 122, 124, 126, 127, 129, 131, 133, 135, 137, 138, 140, 142,
  144, 146, 148, 150, 152, 154, 156, 158, 160, 162, 164, 167, 169, 171, 173, 175,
  177, 180, 182, 184, 186, 189, 191, 193, 196, 198, 200, 203, 205, 208, 210, 213,
  215, 218, 220, 223, 225, 228, 231, 233, 236, 239, 241, 244, 247, 249, 252, 255
};


int delayval = 500; // delay for half a second
int wait = 100;
int shu_wait = 50;
int shu_tail = 3;

int mycolor = 10;

int counter = 1, counterold = 1, canpaicounter = 1, canpaicounterold = 1;
int loopcounter, canpailoopcounter;
unsigned long time0, canpaitime0;

struct accelFlags {
  int counter, counter_old, loopcounter;
  unsigned long time0;
} canpai, ikki;

MPU6050 mpu;
bool blinkState = false;

// MPU control/status vars
bool dmpReady = false;  // set true if DMP init was successful
uint8_t mpuIntStatus;   // holds actual interrupt status byte from MPU
uint8_t devStatus;      // return status after each device operation (0 = success, !0 = error)
uint16_t packetSize;    // expected DMP packet size (default is 42 bytes)
uint16_t fifoCount;     // count of all bytes currently in FIFO
uint8_t fifoBuffer[64]; // FIFO storage buffer

// orientation/motion vars
Quaternion q;           // [w, x, y, z]         quaternion container
VectorInt16 aa;         // [x, y, z]            accel sensor measurements
VectorInt16 aaReal;     // [x, y, z]            gravity-free accel sensor measurements
VectorInt16 aaWorld;    // [x, y, z]            world-frame accel sensor measurements
VectorFloat gravity;    // [x, y, z]            gravity vector
float euler[3];         // [psi, theta, phi]    Euler angle container
float ypr[3];           // [yaw, pitch, roll]   yaw/pitch/roll container and gravity vector


void check_commands(String S_input);//プロトタイプ宣言
String read_command_from_serial();
void write_data_to_rom(String String_data);
String read_data_from_rom(void);
void read_data_form_mpu6050(void);

volatile bool mpuInterrupt = false;     // indicates whether MPU interrupt pin has gone high

void setup() {
  Serial.begin(9600);
  Serial.setTimeout(1000);
  Wire.begin();
  Wire.setClock(400000); // 400kHz I2C clock. Comment this line if having compilation difficulties

  pinMode(POWER_ON, OUTPUT);
  pinMode(CMD, OUTPUT);
  pinMode(12, OUTPUT);
  pinMode(7, OUTPUT);
  digitalWrite(CMD, HIGH);
  digitalWrite(POWER_ON, HIGH);//RN52の電源起動動作
  delay(1500);
  digitalWrite(POWER_ON, LOW);
  digitalWrite(7,HIGH);

  // initialize device
  Serial.println(F("Initializing I2C devices..."));
  delay(500);
  mpu.initialize();
  pinMode(INTERRUPT_PIN, INPUT);

  // verify connection
  Serial.println(F("Testing device connections..."));
  Serial.println(mpu.testConnection() ? F("MPU6050 connection successful") : F("MPU6050 connection failed"));

  // load and configure the DMP
  Serial.println(F("Initializing DMP..."));
  devStatus = mpu.dmpInitialize();

  // supply your own gyro offsets here, scaled for min sensitivity
  mpu.setXGyroOffset(220);
  mpu.setYGyroOffset(76);
  mpu.setZGyroOffset(-85);
  mpu.setZAccelOffset(1788); // 1688 factory default for my test chip

  // make sure it worked (returns 0 if so)
  if (devStatus == 0) {
    // turn on the DMP, now that it's ready
    Serial.println(F("Enabling DMP..."));
    mpu.setDMPEnabled(true);

    // enable Arduino interrupt detection
    Serial.println(F("Enabling interrupt detection (Arduino external interrupt 0)..."));
    attachInterrupt(digitalPinToInterrupt(INTERRUPT_PIN), dmpDataReady, RISING);
    mpuIntStatus = mpu.getIntStatus();

    // set our DMP Ready flag so the main loop() function knows it's okay to use it
    Serial.println(F("DMP ready! Waiting for first interrupt..."));
    dmpReady = true;

    // get expected DMP packet size for later comparison
    packetSize = mpu.dmpGetFIFOPacketSize();
  } else {
    // ERROR!
    // 1 = initial memory load failed
    // 2 = DMP configuration updates failed
    // (if it's going to break, usually the code will be 1)
    Serial.print(F("DMP Initialization failed (code "));
    Serial.print(devStatus);
    Serial.println(F(")"));
  }

  // configure LED for output
  pinMode(LED_PIN, OUTPUT);


#if defined (__AVR_ATtiny85__)
  if (F_CPU == 16000000) clock_prescale_set(clock_div_1);
#endif
  strip.setBrightness(BRIGHTNESS);
  strip.begin();
  strip.show();
  pika_red();
}

VectorInt16 myRealaccel[Array] ;

void loop() {

  if (mpuInterrupt) {
    int i;
    for (i = 0; i < Array; i++) {
      myRealaccel[i + 1] = myRealaccel[i];
    }
    myRealaccel[0] = read_realaccel_form_mpu6050();
  }

  /////////////////////////////////////////CANPAI_DETECTION/////////////////////////////////

  if (CANPAI_THRESHOLD <= ((long int)myRealaccel[0].x * (long int)myRealaccel[0].x + (long int)myRealaccel[0].y * (long int)myRealaccel[0].y)) {
    /*Serial.print("kanpai!" + (String)counter);
      Serial.println("loopc" + (String)loopcounter);
      Serial.println(millis());
      Serial.println(time0);*/
    if (counter == 1 && millis() - time0 > CANPAI_WAITTIME) {
      Serial.println("kanpai!!!");
      time0 = millis();
    }
    ++counter;

  } else if (counterold != 1) {
    counter = 1;
    ++loopcounter;
  }
  counterold = counter;

  ///////////////////////////////////////////////////////////////////////////////////////////


  //////////////////////////////////////////IKKINOMI_DETECTION/////////////////////////////////

  /*if (4000 <= (myRealaccel.z)) {
    Serial.print("ikkistart!" + (String)canpaicounter);
    Serial.println("ikkiloopc" + (String)canpailoopcounter);
    Serial.println(millis());
    Serial.println(canpaitime0);
    ++canpaicounter;

    } else if (canpaicounterold != 1) {
    canpaicounter = 1;
    ++canpailoopcounter;
    if (millis() - canpaitime0 > 500) {
      Serial.println("ikkistart!!!");
    }
    canpaitime0 = millis();
    }
    canpaicounterold = canpaicounter;*/

  //////////////////////////////////////////////////////////////////////////////////////////////////
}

void dmpDataReady() {//加速度を受信したとき、割り込み処理を行う
  mpuInterrupt = true;
}

void serialEvent() { //シリアル通信を受信したとき、割り込み処理を行う
  if (Serial.available()) {
    check_commands(read_command_from_serial());
  }
}

String read_command_from_serial() {
  String S_input;
  S_input = Serial.readString();
  char* p;
  unsigned long time0 = millis();
  for (p = &S_input[0]; *p != '\r' && *p != '\n' && *p != '\0'; p++) { //改行文字などを消す
    if (millis() - time0 > 1000) {
      Serial.println("ERROR:Serial_timeouted");
      break;
    }
  }
  *p = '\0';
  return &S_input[0];
}

VectorInt16 read_realaccel_form_mpu6050(void) {
  mpuInterrupt = false;

  mpuIntStatus = mpu.getIntStatus();

  fifoCount = mpu.getFIFOCount();

  // check for overflow
  if ((mpuIntStatus & 0x10) || fifoCount == 1024) {
    // reset so we can continue cleanly
    mpu.resetFIFO();
    //Serial.println(F("FIFO overflow!"));

    // otherwise, check for DMP data ready interrupt (this should happen frequently)
  } else if (mpuIntStatus & 0x02) {


    // wait for correct available data length, should be a VERY short wait
    while (fifoCount < packetSize) fifoCount = mpu.getFIFOCount();

    // read a packet from FIFO
    mpu.getFIFOBytes(fifoBuffer, packetSize);

    // display real acceleration, adjusted to remove gravity
    mpu.dmpGetQuaternion(&q, fifoBuffer);
    mpu.dmpGetAccel(&aa, fifoBuffer);
    mpu.dmpGetGravity(&gravity, &q);
    mpu.dmpGetLinearAccel(&aaReal, &aa, &gravity);

    blinkState = !blinkState;
    digitalWrite(LED_PIN, blinkState);

    return aaReal;
  }
}

void check_commands(String S_input) {
  if (S_input.startsWith("set,")) {
    String String_data = S_input.substring(4);
    write_data_to_rom(String_data);
    String data_from_rom = read_data_from_rom();
    Serial.println("saved");
    Serial.println("current," + data_from_rom);
  }
  else if (S_input.equals("get")) {
    String data_from_rom = read_data_from_rom();
    Serial.println("current," + data_from_rom);
  }
  else if (S_input.equals("ledon")) {
    pika_blue();
    Serial.println("Led on");
  }
  else if (S_input.equals("ledoff")) {
    no_light();
    Serial.println("Led off");
  }
  else {
    Serial.println(S_input + " is not command");
  }
}

void write_data_to_rom(String String_data) {
  byte Byte_data[30];
  String_data.getBytes(Byte_data, 30);
  int k;
  for (k = 0; k < 30 && Byte_data[k] != '\0'; k++) {
    EEPROM.write(k, Byte_data[k]);
  }
  EEPROM.write(k, '\0');
}

String read_data_from_rom(void) {
  String data_from_rom;
  for (int i = 0; i < 30 && EEPROM.read(i) != '\0' ; i++) {
    char c = EEPROM.read(i);
    data_from_rom = data_from_rom + String(c);
  }
  return data_from_rom;
}

void pika_red() {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, strip.Color(0, 255, 0));
    strip.show();
  }
}

void pika_green() {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, strip.Color(255, 0, 0));
    strip.show();
  }
}

void pika_blue() {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, strip.Color(0, 0, 255));
    strip.show();
  }
}

void pika_rainbow() {
  uint16_t i, j;

  for (j = 0; j < 256; j++) {
    for (i = 0; i < strip.numPixels(); i++) {
      strip.setPixelColor(i, Wheel((i + j) & 255));
    }
    strip.show();
    delay(wait);
  }
}

void pika_rainbow2() {
  int i, j;

  for (j = 0; j < 256; j++) {
    for (i = 0; i < strip.numPixels(); i++) {
      strip.setPixelColor(i, Wheel((i + j) & 255));
    }
    strip.show();
    delay(wait);
  }
}

void shu_red() {
  for (int i = 0; i < strip.numPixels() + shu_tail + 1; i++) {
    if (i >= strip.numPixels()) {
      //先頭が全体のLED数の先を行く時
      strip.setPixelColor(i - (shu_tail + 1), strip.Color(0, 0, 0));
      strip.show();
    } else {
      strip.setPixelColor(i, strip.Color(0, 255, 0));
      strip.show();
      if (i < shu_tail + 1) {
        //何もせず
      } else {
        strip.setPixelColor(i - (shu_tail + 1), strip.Color(0, 0, 0));
        strip.show();
      }
    }
    delay(shu_wait);
  }
}


void ziwaziwa() {
  for (int j = 0; j < 256; j++) {
    for (int i = 0; i < strip.numPixels(); i++) {
      //strip.setPixel(i, Wheel(mycolor & 255));
    }
    strip.show();
  }
}

void no_light() {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, strip.Color(0, 0, 0));
    strip.show();
  }
}

uint32_t Wheel(byte WheelPos) {
  WheelPos = 255 - WheelPos;
  if (WheelPos < 85) {
    return strip.Color(0, 255 - WheelPos * 3, WheelPos * 3, 0);
  }
  if (WheelPos < 170) {
    WheelPos -= 85;
    return strip.Color(WheelPos * 3, 0, 255 - WheelPos * 3, 0);
  }
  WheelPos -= 170;
  return strip.Color(WheelPos * 3, 255 - WheelPos * 3, 0, 0);
}
