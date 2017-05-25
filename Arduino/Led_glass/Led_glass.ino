
#include <EEPROM.h>
#include <Wire.h>
#define POWER_ON 9
#define CMD 7

void check_commands(String S_input);//プロトタイプ宣言
String read_command_from_serial();
void write_data_to_rom(String String_data);
String read_data_from_rom(void);
void read_data_form_mpu6050(void);

void setup() {
  Serial.begin(9600);
  Serial.setTimeout(500);
  Wire.begin();
  pinMode(POWER_ON, OUTPUT);
  pinMode(CMD, OUTPUT);
  pinMode(12, OUTPUT);
  digitalWrite(CMD, HIGH);
  digitalWrite(POWER_ON, HIGH);//RN52の電源起動動作
  delay(1500);
  digitalWrite(POWER_ON, LOW);
}

void loop() {
  if(Wire.available()){
    read_data_form_mpu6050();
  }
}

void serialEvent(){//シリアル通信を受信したとき、割り込み処理を行う
  if(Serial.available()) {
    check_commands(read_command_from_serial());
  }
}

String read_command_from_serial() {
  String S_input;
  S_input = Serial.readString();
  char* p;
  unsigned long time0 = millis();
  for (p = &S_input[0]; *p != '\r' && *p !='\n' && *p != '\0'; p++) {//改行文字などを消す
    if(millis()-time0>1000){
      Serial.println("ERROR:Serial_timeouted");
      break;
    }
  }
  *p = '\0';
  return &S_input[0];
}

void read_data_form_mpu6050(void){
  
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
    digitalWrite(12, HIGH);
    delay(1000);
    digitalWrite(12, LOW);
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

