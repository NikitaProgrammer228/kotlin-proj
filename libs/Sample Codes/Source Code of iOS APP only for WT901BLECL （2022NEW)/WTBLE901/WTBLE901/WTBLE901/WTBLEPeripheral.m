//
//  WTBLEPeripheral.m
//  WTBLE901
//
//  Created by wit-motion on 2019/1/29.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import "WTBLEPeripheral.h"

@implementation WTBLEPeripheral

+ (WTBLEPeripheral *)peripheralWithCBPeripheral:(CBPeripheral *)peripheral
                              advertisementData:(NSDictionary *)advertisementData
                                           RSSI:(NSNumber *)RSSI {
    if (peripheral == nil) {
        return nil;
    }
    
    WTBLEPeripheral *blePeripheral = [[WTBLEPeripheral alloc] init];
    blePeripheral.peripheral = peripheral;
    blePeripheral.advertisementData = advertisementData;
    blePeripheral.RSSI = RSSI;
    return blePeripheral;
}

@end
