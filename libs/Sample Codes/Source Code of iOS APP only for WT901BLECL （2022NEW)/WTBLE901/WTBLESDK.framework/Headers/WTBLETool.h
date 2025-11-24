//
//  WTBLETool.h
//  WTBLESDK
//
//  Created by wit-motion on 2019/1/25.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

@interface WTBLETool : NSObject

//十六进制转换为普通字符串的。
+ (NSString *)ConvertHexStringToString:(NSString *)hexString;
//普通字符串转换为十六进制
+ (NSString *)ConvertStringToHexString:(NSString *)string;
//int转data
+(NSData *)ConvertIntToData:(int)i;
//data转int
+(int)ConvertDataToInt:(NSData *)data;
//data转byte
+ (Byte *)ConvertDataToByte:(NSData *)data;
//十六进制转换为普通字符串的。
+ (NSData *)ConvertHexStringToData:(NSString *)hexString;
//根据UUIDString查找CBCharacteristic
+(CBCharacteristic *)findCharacteristicFormServices:(NSMutableArray *)services
                                         UUIDString:(NSString *)UUIDString;

@end
