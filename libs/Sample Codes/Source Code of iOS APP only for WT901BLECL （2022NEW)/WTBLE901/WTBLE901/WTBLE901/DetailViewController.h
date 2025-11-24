//
//  DetailViewController.h
//  WTBLE901
//
//  Created by wit-motion on 2019/1/29.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "WTBLEPeripheral.h"

@interface DetailViewController : UIViewController

@property (nonatomic, strong) WTBLEPeripheral *peripheral;

@end
