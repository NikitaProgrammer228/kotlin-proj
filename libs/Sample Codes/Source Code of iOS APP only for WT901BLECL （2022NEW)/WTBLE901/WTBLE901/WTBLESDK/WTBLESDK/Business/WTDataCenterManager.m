//
//  WTDataCenterManager.m
//  WTBLESDK
//
//  Created by wit-motion on 2019/1/26.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import "WTDataCenterManager.h"

@implementation WTDataCenterManager

- (instancetype)init {
    if (self = [super init]) {
        self.callback = [[WTDataCenterCallback alloc] init];
    }
    return self;
}

- (void)dealWithReadCharacteristicValue:(NSData *)data {
    NSInteger length = data.length;
    Byte byte[length];
    [data getBytes:byte length:length];
    int header = byte[0] & 0xff;
    int flag = byte[1] & 0xff;
    
    if (header != 0x55) {
        NSLog(@"not support characteristic value");
        return;
    }
    if (flag == 0x61) {
        if (length != 20) {
            NSLog(@"0x61 not in correct format");
            return;
        }
                
        double ax = (short)((byte[3] << 8) | (byte[2] & 0xff)) / 32768.0 * 16;
        double ay = (short)((byte[5] << 8) | (byte[4] & 0xff)) / 32768.0 * 16;
        double az = (short)((byte[7] << 8) | (byte[6] & 0xff)) / 32768.0 * 16;
        double a = sqrt(ax*ax + ay*ay + az*az);
        if (self.callback.blockOnUpdateAcc) {
            self.callback.blockOnUpdateAcc(ax, ay, az, a);
        }
        
        double wx = (short)((byte[9] << 8) | (byte[8] & 0xff)) / 32768.0 * 2000;
        double wy = (short)((byte[11] << 8) | (byte[10] & 0xff)) / 32768.0 * 2000;
        double wz = (short)((byte[13] << 8) | (byte[12] & 0xff)) / 32768.0 * 2000;
        double w = sqrt(wx*wx + wy*wy + wz*wz);
        if (self.callback.blockOnUpdateAngularV) {
            self.callback.blockOnUpdateAngularV(wx, wy, wz, w);
        }
        
        double roll = (short)((byte[15] << 8) | (byte[14] & 0xff)) / 32768.0 * 180;
        double pitch = (short)((byte[17] << 8) | (byte[16] & 0xff)) / 32768.0 * 180;
        double yaw = (short)((byte[19] << 8) | (byte[18] & 0xff)) / 32768.0 * 180;
        if (self.callback.blockOnUpdateAngle) {
            self.callback.blockOnUpdateAngle(roll, pitch, yaw);
        }
    } else if (flag == 0x71) {
        int regH = byte[2] & 0xff;
        int regL = byte[3] & 0xff;
        
        if (regH == 0x3A && regL == 0x00) {
            double mx = (short)((byte[5] << 8) | (byte[4] & 0xff));
            double my = (short)((byte[7] << 8) | (byte[6] & 0xff));
            double mz = (short)((byte[9] << 8) | (byte[8] & 0xff));
            double m = sqrt(mx*mx + my*my + mz*mz);
            if (self.callback.blockOnUpdateMagnetic) {
                self.callback.blockOnUpdateMagnetic(mx, my, mz, m);
            }
        } else if (regH == 0x45 && regL == 0x00) {
            int p = (byte[7] << 24) | (byte[6] << 16) | (byte[5] << 8) | (byte[4]);
            int h = (byte[11] << 24) | (byte[10] << 16) | (byte[9] << 8) | (byte[8]);
            if (self.callback.blockOnUpdatePressure) {
                self.callback.blockOnUpdatePressure(p, h);
            }
        } else if (regH == 0x41 && regL == 0x00) {
            double d0 = (short)((byte[5] << 8) | (byte[4] & 0xff));
            double d1 = (short)((byte[7] << 8) | (byte[6] & 0xff));
            double d2 = (short)((byte[9] << 8) | (byte[8] & 0xff));
            double d3 = (short)((byte[11] << 8) | (byte[10] & 0xff));
            if (self.callback.blockOnUpdatePort) {
                self.callback.blockOnUpdatePort(d0, d1, d2, d3);
            }
        } else if (regH == 0x51 && regL == 0x00) {
            double q0 = (short)((byte[5] << 8) | (byte[4] & 0xff)) / 32768.0;
            double q1 = (short)((byte[7] << 8) | (byte[6] & 0xff)) / 32768.0;
            double q2 = (short)((byte[9] << 8) | (byte[8] & 0xff)) / 32768.0;
            double q3 = (short)((byte[11] << 8) | (byte[10] & 0xff)) / 32768.0;
            if (self.callback.blockOnUpdateQuaternion) {
                self.callback.blockOnUpdateQuaternion(q0, q1, q2, q3);
            }
        } else if (regH == 0x40 && regL == 0x00) {
            double t = (short)((byte[5] << 8) | (byte[4] & 0xff)) / 100.0;
            if (self.callback.blockOnUpdateTempurature) {
                self.callback.blockOnUpdateTempurature(t);
            }
        }
    }
}

- (NSData *)assembleMagneticCommand {
    Byte byte[] = {0xFF, 0xAA, 0x27, 0x3A, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleBaromemtricPressureCommand {
    Byte byte[] = {0xFF, 0xAA, 0x27, 0x45, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assemblePortCommand {
    Byte byte[] = {0xFF, 0xAA, 0x27, 0x41, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleQuaternionCommand {
    Byte byte[] = {0xFF, 0xAA, 0x27, 0x51, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleTemperatureCommand {
    Byte byte[] = {0xFF, 0xAA, 0x27, 0x40, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleAccCaliCommand {
    Byte byte[] = {0xFF, 0xAA, 0x01, 0x01, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleAccLCaliCommand {
    Byte byte[] = {0xFF, 0xAA, 0x01, 0x05, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleAccRCaliCommand {
    Byte byte[] = {0xFF, 0xAA, 0x01, 0x06, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleMagneticCaliCommand {
    Byte byte[] = {0xFF, 0xAA, 0x01, 0x07, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleFinishMagneticCaliCommand {
    Byte byte[] = {0xFF, 0xAA, 0x01, 0x00, 0x00};
    return [NSData dataWithBytes:byte length:5];
}

- (NSData *)assembleSaveCommand:(BOOL)isDefault {
    if (isDefault) {
        Byte byte[] = {0xFF, 0xAA, 0x00, 0x01, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else {
        Byte byte[] = {0xFF, 0xAA, 0x00, 0x00, 0x00};
        return [NSData dataWithBytes:byte length:5];
    }
}

- (NSData *)assembleRateCommandWithRate:(float)rate {
    if (fabs(rate - 0.1) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x01, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (fabs(rate - 0.5) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x02, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (fabs(rate - 1) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x03, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (fabs(rate - 2) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x04, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (fabs(rate - 5) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x05, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (fabs(rate - 10) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x06, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (fabs(rate - 20) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x07, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (fabs(rate - 50) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x08, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (fabs(rate - 100) < 0.0001) {
        Byte byte[] = {0xFF, 0xAA, 0x03, 0x09, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else {
        return nil;
    }
}

- (NSData *)assembleD0Command:(WTPortMode)portMode {
    if (portMode == WTPortMode_DigitalInput) {
        Byte byte[] = {0xFF, 0xAA, 0x0E, 0x01, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (portMode == WTPortMode_HighLevelOutput) {
        Byte byte[] = {0xFF, 0xAA, 0x0E, 0x02, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (portMode == WTPortMode_LowLevelOutput) {
        Byte byte[] = {0xFF, 0xAA, 0x0E, 0x03, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else {
        // WTPortMode_AnalogInput default
        Byte byte[] = {0xFF, 0xAA, 0x0E, 0x00, 0x00};
        return [NSData dataWithBytes:byte length:5];
    }
}

- (NSData *)assembleD1Command:(WTPortMode)portMode {
    if (portMode == WTPortMode_DigitalInput) {
        Byte byte[] = {0xFF, 0xAA, 0x0F, 0x01, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (portMode == WTPortMode_HighLevelOutput) {
        Byte byte[] = {0xFF, 0xAA, 0x0F, 0x02, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (portMode == WTPortMode_LowLevelOutput) {
        Byte byte[] = {0xFF, 0xAA, 0x0F, 0x03, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else {
        // WTPortMode_AnalogInput default
        Byte byte[] = {0xFF, 0xAA, 0x0F, 0x00, 0x00};
        return [NSData dataWithBytes:byte length:5];
    }
}

- (NSData *)assembleD2Command:(WTPortMode)portMode {
    if (portMode == WTPortMode_DigitalInput) {
        Byte byte[] = {0xFF, 0xAA, 0x10, 0x01, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (portMode == WTPortMode_HighLevelOutput) {
        Byte byte[] = {0xFF, 0xAA, 0x10, 0x02, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (portMode == WTPortMode_LowLevelOutput) {
        Byte byte[] = {0xFF, 0xAA, 0x10, 0x03, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else {
        // WTPortMode_AnalogInput default
        Byte byte[] = {0xFF, 0xAA, 0x10, 0x00, 0x00};
        return [NSData dataWithBytes:byte length:5];
    }
}

- (NSData *)assembleD3Command:(WTPortMode)portMode {
    if (portMode == WTPortMode_DigitalInput) {
        Byte byte[] = {0xFF, 0xAA, 0x11, 0x01, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (portMode == WTPortMode_HighLevelOutput) {
        Byte byte[] = {0xFF, 0xAA, 0x11, 0x02, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else if (portMode == WTPortMode_LowLevelOutput) {
        Byte byte[] = {0xFF, 0xAA, 0x11, 0x03, 0x00};
        return [NSData dataWithBytes:byte length:5];
    } else {
        // WTPortMode_AnalogInput default
        Byte byte[] = {0xFF, 0xAA, 0x11, 0x00, 0x00};
        return [NSData dataWithBytes:byte length:5];
    }
}

- (NSData *)assembleChangeNameCommand:(NSString *)name {
    NSString *commandStr = [NSString stringWithFormat:@"WT%@\r\n", name];
    return [commandStr dataUsingEncoding:NSUTF8StringEncoding];
}

@end
