//
//  DetailViewController.m
//  WTBLE901
//
//  Created by wit-motion on 2019/1/29.
//  Copyright © 2019 wit-motion. All rights reserved.
//

#import "DetailViewController.h"

typedef NS_ENUM(NSInteger, ItemDataType) {
    ItemDataType_Acceleration = 1,
    ItemDataType_AngularVelocity,
    ItemDataType_Angle,
    ItemDataType_Magnetic,
    ItemDataType_BaromemtricPressure,
    ItemDataType_Port,
    ItemDataType_Quaternion
};

@interface ItemDataModel : NSObject

@property (nonatomic, assign) ItemDataType dataType;
@property (nonatomic, copy) NSString *title;

+ (ItemDataModel *)itemDataWithTitle:(NSString *)title type:(ItemDataType)type;
+ (NSArray<ItemDataModel *> *)itemDataList;

@end

@implementation ItemDataModel

+ (ItemDataModel *)itemDataWithTitle:(NSString *)title type:(ItemDataType)type {
    ItemDataModel *item = [[ItemDataModel alloc] init];
    item.title = title;
    item.dataType = type;
    return item;
}

+ (NSArray<ItemDataModel *> *)itemDataList {
    return @[[ItemDataModel itemDataWithTitle:NSLocalizedString(@"acceleration", nil) type:ItemDataType_Acceleration],
             [ItemDataModel itemDataWithTitle:NSLocalizedString(@"angular_velocity", nil) type:ItemDataType_AngularVelocity],
             [ItemDataModel itemDataWithTitle:NSLocalizedString(@"angle", nil) type:ItemDataType_Angle],
             [ItemDataModel itemDataWithTitle:NSLocalizedString(@"magnetic", nil) type:ItemDataType_Magnetic],
             [ItemDataModel itemDataWithTitle:NSLocalizedString(@"barometric_pressure", nil) type:ItemDataType_BaromemtricPressure],
             [ItemDataModel itemDataWithTitle:NSLocalizedString(@"port", nil) type:ItemDataType_Port],
             [ItemDataModel itemDataWithTitle:NSLocalizedString(@"quaternion", nil) type:ItemDataType_Quaternion],];
}

@end

@interface HeaderItemCell : UICollectionViewCell

@property (nonatomic, strong) UILabel *titleLabel;

@end

@implementation HeaderItemCell

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.titleLabel = [[UILabel alloc] init];
        self.titleLabel.textAlignment = NSTextAlignmentCenter;
        self.titleLabel.textColor = [UIColor whiteColor];
        [self addSubview:self.titleLabel];
    }
    return self;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    self.titleLabel.frame = self.bounds;
}

- (void)setSelected:(BOOL)selected {
    if (selected) {
        self.contentView.backgroundColor = [UIColor colorWithRed:95/255.0 green:158/255.0 blue:160/255.0 alpha:1.0];
    } else {
        self.contentView.backgroundColor = [UIColor clearColor];
    }
}

@end

@interface DetailViewController ()<UICollectionViewDataSource, UICollectionViewDelegate>

@property (nonatomic, strong) UIButton *connectButton;
@property (nonatomic, strong) NSArray<ItemDataModel *> *itemDataList;
@property (nonatomic, strong) UICollectionView *headerItemView;
@property (nonatomic, strong) UICollectionViewFlowLayout *collectionViewLayout;

@property (weak, nonatomic) IBOutlet UILabel *itemTitleLabel0;
@property (weak, nonatomic) IBOutlet UILabel *itemValueLabel0;

@property (weak, nonatomic) IBOutlet UILabel *itemTitleLabel1;
@property (weak, nonatomic) IBOutlet UILabel *itemValueLabel1;

@property (weak, nonatomic) IBOutlet UILabel *itemTitleLabel2;
@property (weak, nonatomic) IBOutlet UILabel *itemValueLabel2;

@property (weak, nonatomic) IBOutlet UILabel *itemTitleLabel3;
@property (weak, nonatomic) IBOutlet UILabel *itemValueLabel3;

@property (nonatomic, assign) ItemDataType selectDataType;
@property (nonatomic, assign) BOOL isFirstTimeLoad;

@property (nonatomic, strong) NSTimer *refreshTimer;

@end

@implementation DetailViewController

- (UIButton *)connectButton {
    if (!_connectButton) {
        _connectButton = [UIButton buttonWithType:UIButtonTypeCustom];
        [_connectButton addTarget:self action:@selector(onConnectButtonAction:) forControlEvents:UIControlEventTouchUpInside];
        [_connectButton setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
        [_connectButton setTitleColor:[UIColor blackColor] forState:UIControlStateSelected];
        [_connectButton setTitle:NSLocalizedString(@"disconnect", nil) forState:UIControlStateNormal];
        [_connectButton setTitle:NSLocalizedString(@"connect", nil) forState:UIControlStateSelected];
    }
    return _connectButton;
}

- (UICollectionView *)headerItemView {
    if (!_headerItemView) {
        _headerItemView = [[UICollectionView alloc] initWithFrame:CGRectZero collectionViewLayout:self.collectionViewLayout];
        _headerItemView.delegate = self;
        _headerItemView.dataSource = self;
        _headerItemView.backgroundColor = [UIColor whiteColor];
        _headerItemView.showsHorizontalScrollIndicator = NO;
        _headerItemView.showsVerticalScrollIndicator = NO;
        _headerItemView.alwaysBounceHorizontal = NO;
        _headerItemView.backgroundColor = [UIColor colorWithRed:28/255.0 green:184/255.0 blue:255/255.0 alpha:1.0];
        [_headerItemView registerClass:[HeaderItemCell class]
          forCellWithReuseIdentifier:[self cellReuseIdentifier]];
    }
    return _headerItemView;
}

- (NSString *)cellReuseIdentifier {
    return NSStringFromClass([HeaderItemCell class]);
}

- (UICollectionViewFlowLayout *)collectionViewLayout {
    if (!_collectionViewLayout) {
        _collectionViewLayout = [[UICollectionViewFlowLayout alloc] init];
        _collectionViewLayout.itemSize = CGSizeMake(80, 44);
        _collectionViewLayout.minimumLineSpacing = 0;
        _collectionViewLayout.minimumInteritemSpacing = 0;
        _collectionViewLayout.scrollDirection = UICollectionViewScrollDirectionHorizontal;
    }
    return _collectionViewLayout;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.title = [self.peripheral.advertisementData objectForKey:@"kCBAdvDataLocalName"] ?: (self.peripheral.peripheral.name ?: @"Unknown device");
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithCustomView:self.connectButton];
    self.itemDataList = [ItemDataModel itemDataList];
    
    [self.view addSubview:self.headerItemView];
    
    if (@available(iOS 11.0, *)) {
        CGFloat topLayoutGuide = [UIApplication sharedApplication].keyWindow.safeAreaInsets.top ? [UIApplication sharedApplication].keyWindow.safeAreaInsets.top : 20;
        self.headerItemView.frame = CGRectMake(0, topLayoutGuide + 44, [UIScreen mainScreen].bounds.size.width, 44);
    } else {
        self.headerItemView.frame = CGRectMake(0, 64, [UIScreen mainScreen].bounds.size.width, 44);
    }
    
    self.selectDataType = ItemDataType_Acceleration;
    self.isFirstTimeLoad = YES;
    [self connect];
    __weak typeof(DetailViewController) *weakSelf = self;
    
    [WTBLESDK sharedInstance].bleCallback.blockOnConnectedPeripheral = ^(CBCentralManager *central, CBPeripheral *peripheral) {
        [weakSelf tryReceiveData];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdateAngularV = ^(double wx, double wy, double wz, double total) {
        [weakSelf updateAngularYWithWx:wx wy:wy wz:wz total:total];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdateAcc = ^(double ax, double ay, double az, double total) {
        [weakSelf updateAccWithAx:ax ay:ay az:az total:total];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdateAngle = ^(double roll, double pitch, double yaw) {
        [weakSelf updateAngleWith:roll pitch:pitch yaw:yaw];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdateTempurature = ^(double t) {
        [weakSelf updateTempurature:t];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdateMagnetic = ^(double mx, double my, double mz, double m) {
        [weakSelf updateMagneticWith:mx my:my mz:mz total:m];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdatePressure = ^(double p, double h) {
        [weakSelf updatePressure:p altitude:h];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdatePort = ^(double d0, double d1, double d2, double d3) {
        [weakSelf updatePortWith:d0 d1:d1 d2:d2 d3:d3];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdateQuaternion = ^(double q0, double q1, double q2, double q3) {
        [weakSelf updateQuaternionWith:q0 q1:q1 q2:q2 q3:q3];
    };
    
    [WTBLESDK sharedInstance].dataCallback.blockOnUpdateBLEName = ^(BOOL isSuccess, NSError *error) {
        [weakSelf dealWithUpdateBLENameInfo:error];
    };
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [self cancelConnect];
}

- (void)connect {
    [[WTBLESDK sharedInstance] tryConnectPeripheral:self.peripheral.peripheral];
}

- (void)cancelConnect {
    [[WTBLESDK sharedInstance] cancelConnection];
    if (self.refreshTimer) {
        [self.refreshTimer invalidate];
        self.refreshTimer = nil;
    }
}

- (void)tryReceiveData {
    [[WTBLESDK sharedInstance] tryReceiveDataAfterConnected];
    
    if (!self.refreshTimer) {
        self.refreshTimer = [NSTimer scheduledTimerWithTimeInterval:0.2
                                                             target:self
                                                           selector:@selector(onRefreshTimerTick:)
                                                           userInfo:nil
                                                            repeats:YES];
    }
}

- (void)onRefreshTimerTick:(NSTimer *)timer {
    if (self.selectDataType == ItemDataType_Angle) {
        [[WTBLESDK sharedInstance] sendTempuratureCommand];
    } else if (self.selectDataType == ItemDataType_Magnetic) {
        [[WTBLESDK sharedInstance] sendMagneticCommand];
    } else if (self.selectDataType == ItemDataType_BaromemtricPressure) {
        [[WTBLESDK sharedInstance] sendBaromemtricPressureCommand];
    } else if (self.selectDataType == ItemDataType_Port) {
        [[WTBLESDK sharedInstance] sendPortCommand];
    } else if (self.selectDataType == ItemDataType_Quaternion) {
        [[WTBLESDK sharedInstance] sendQuaternionCommand];
    }
}

- (void)setSelectDataType:(ItemDataType)selectDataType {
    if (_selectDataType == selectDataType) {
        return;
    }
    
    _selectDataType = selectDataType;
    self.itemTitleLabel0.text = nil;
    self.itemTitleLabel1.text = nil;
    self.itemTitleLabel2.text = nil;
    self.itemTitleLabel3.text = nil;
    self.itemValueLabel0.text = nil;
    self.itemValueLabel1.text = nil;
    self.itemValueLabel2.text = nil;
    self.itemValueLabel3.text = nil;
}

#pragma mark - update show values

- (void)updateAccWithAx:(double)ax ay:(double)ay az:(double)az total:(double)total {
    if (self.selectDataType != ItemDataType_Acceleration) {
        return;
    }
    self.itemTitleLabel0.text = @"ax:";
    self.itemTitleLabel1.text = @"ay:";
    self.itemTitleLabel2.text = @"az:";
    self.itemTitleLabel3.text = @"|a|:";
    self.itemValueLabel0.text = [NSString stringWithFormat:@"%0.2fg", ax];
    self.itemValueLabel1.text = [NSString stringWithFormat:@"%0.2fg", ay];
    self.itemValueLabel2.text = [NSString stringWithFormat:@"%0.2fg", az];
    self.itemValueLabel3.text = [NSString stringWithFormat:@"%0.2fg", total];
}

- (void)updateAngularYWithWx:(double)wx wy:(double)wy wz:(double)wz total:(double)total {
    if (self.selectDataType != ItemDataType_AngularVelocity) {
        return;
    }
    self.itemTitleLabel0.text = @"wx:";
    self.itemTitleLabel1.text = @"wy:";
    self.itemTitleLabel2.text = @"wz:";
    self.itemTitleLabel3.text = @"|w|:";
    self.itemValueLabel0.text = [NSString stringWithFormat:@"%0.2f°/s", wx];
    self.itemValueLabel1.text = [NSString stringWithFormat:@"%0.2f°/s", wy];
    self.itemValueLabel2.text = [NSString stringWithFormat:@"%0.2f°/s", wz];
    self.itemValueLabel3.text = [NSString stringWithFormat:@"%0.2f°/s", total];
}

//roll, double pitch, double yaw
- (void)updateAngleWith:(double)roll pitch:(double)pitch yaw:(double)yaw {
    if (self.selectDataType != ItemDataType_Angle) {
        return;
    }
    self.itemTitleLabel0.text = @"AngleX:";
    self.itemTitleLabel1.text = @"AngleY:";
    self.itemTitleLabel2.text = @"AngleZ:";
    self.itemValueLabel0.text = [NSString stringWithFormat:@"%0.2f°", roll];
    self.itemValueLabel1.text = [NSString stringWithFormat:@"%0.2f°", pitch];
    self.itemValueLabel2.text = [NSString stringWithFormat:@"%0.2f°", yaw];
}

- (void)updateTempurature:(double)tempurature {
    if (self.selectDataType != ItemDataType_Angle) {
        return;
    }
    self.itemTitleLabel3.text = @"T:";
    self.itemValueLabel3.text = [NSString stringWithFormat:@"%0.2f°C", tempurature];
}

- (void)updateMagneticWith:(double)mx my:(double)my mz:(double)mz total:(double)total {
    if (self.selectDataType != ItemDataType_Magnetic) {
        return;
    }
    
    self.itemTitleLabel0.text = @"hx:";
    self.itemTitleLabel1.text = @"hy:";
    self.itemTitleLabel2.text = @"hz:";
    self.itemTitleLabel3.text = @"|h|:";
    self.itemValueLabel0.text = [NSString stringWithFormat:@"%0.0f", mx];
    self.itemValueLabel1.text = [NSString stringWithFormat:@"%0.0f", my];
    self.itemValueLabel2.text = [NSString stringWithFormat:@"%0.0f", mz];
    self.itemValueLabel3.text = [NSString stringWithFormat:@"%0.0f", total];
}

- (void)updatePressure:(double)pressure altitude:(double)altitude {
    if (self.selectDataType != ItemDataType_BaromemtricPressure) {
        return;
    }
    
    self.itemTitleLabel0.text = @"Pressure:";
    self.itemTitleLabel1.text = @"Height:";
    self.itemTitleLabel2.text = nil;
    self.itemTitleLabel3.text = nil;
    self.itemValueLabel0.text = [NSString stringWithFormat:@"%0.2fPa", pressure];
    self.itemValueLabel1.text = [NSString stringWithFormat:@"%0.2fm", altitude / 100];
    self.itemValueLabel2.text = nil;
    self.itemValueLabel3.text = nil;
}

- (void)updatePortWith:(double)d0 d1:(double)d1 d2:(double)d2 d3:(double)d3 {
    if (self.selectDataType != ItemDataType_Port) {
        return;
    }
    
    self.itemTitleLabel0.text = @"D0:";
    self.itemTitleLabel1.text = @"D1:";
    self.itemTitleLabel2.text = @"D2:";
    self.itemTitleLabel3.text = @"D3:";
    self.itemValueLabel0.text = [NSString stringWithFormat:@"%0.0f", d0];
    self.itemValueLabel1.text = [NSString stringWithFormat:@"%0.0f", d1];
    self.itemValueLabel2.text = [NSString stringWithFormat:@"%0.0f", d2];
    self.itemValueLabel3.text = [NSString stringWithFormat:@"%0.0f", d3];
}

- (void)updateQuaternionWith:(double)q0 q1:(double)q1 q2:(double)q2 q3:(double)q3 {
    if (self.selectDataType != ItemDataType_Quaternion) {
        return;
    }
    
    self.itemTitleLabel0.text = @"q0:";
    self.itemTitleLabel1.text = @"q1:";
    self.itemTitleLabel2.text = @"q2:";
    self.itemTitleLabel3.text = @"q3:";
    self.itemValueLabel0.text = [NSString stringWithFormat:@"%0.3f", q0];
    self.itemValueLabel1.text = [NSString stringWithFormat:@"%0.3f", q1];
    self.itemValueLabel2.text = [NSString stringWithFormat:@"%0.3f", q2];
    self.itemValueLabel3.text = [NSString stringWithFormat:@"%0.3f", q3];
}

#pragma mark - actions

- (void)onConnectButtonAction:(UIButton *)sender {
    sender.selected = !sender.isSelected;
    if (sender.isSelected) {
        [self cancelConnect];
    } else {
        [self connect];
    }
}

- (IBAction)onAccelerometerButtonAction:(id)sender {
    [[WTBLESDK sharedInstance] accelerometerCali];
}

- (IBAction)onAccelerometerLButtonAction:(id)sender {
    [[WTBLESDK sharedInstance] accelerometerCaliL];
}

- (IBAction)onAccelerometerRButtonAction:(id)sender {
    [[WTBLESDK sharedInstance] accelerometerCaliR];
}

- (IBAction)onMagneticButtonAction:(UIButton *)sender {
    if (sender.isSelected) {
        [[WTBLESDK sharedInstance] finishMagneticCali];
    } else {
        [[WTBLESDK sharedInstance] magneticCali];
    }
    
    sender.selected = !sender.isSelected;
}

- (IBAction)onD0ButtonAction:(id)sender {
    [self showActionSheetAlertWithTitle:NSLocalizedString(@"select_port_mode", nil)
                           actionTitles:@[@"AIn", @"DIn", @"DOutH", @"DOutL"]
                        itemSelectBlock:^(NSInteger index)
    {
        if (index == 0) {
            [[WTBLESDK sharedInstance] D0Cali:WTPortMode_AnalogInput];
        } else if (index == 1) {
            [[WTBLESDK sharedInstance] D0Cali:WTPortMode_DigitalInput];
        } else if (index == 2) {
            [[WTBLESDK sharedInstance] D0Cali:WTPortMode_HighLevelOutput];
        } else if (index == 3) {
            [[WTBLESDK sharedInstance] D0Cali:WTPortMode_LowLevelOutput];
        }
    }];
}

- (IBAction)onD1ButtonAction:(id)sender {
    [self showActionSheetAlertWithTitle:NSLocalizedString(@"select_port_mode", nil)
                           actionTitles:@[@"AIn", @"DIn", @"DOutH", @"DOutL"]
                        itemSelectBlock:^(NSInteger index)
     {
         if (index == 0) {
             [[WTBLESDK sharedInstance] D1Cali:WTPortMode_AnalogInput];
         } else if (index == 1) {
             [[WTBLESDK sharedInstance] D1Cali:WTPortMode_DigitalInput];
         } else if (index == 2) {
             [[WTBLESDK sharedInstance] D1Cali:WTPortMode_HighLevelOutput];
         } else if (index == 3) {
             [[WTBLESDK sharedInstance] D1Cali:WTPortMode_LowLevelOutput];
         }
     }];
}

- (IBAction)onD2ButtonAction:(id)sender {
    [self showActionSheetAlertWithTitle:NSLocalizedString(@"select_port_mode", nil)
                           actionTitles:@[@"AIn", @"DIn", @"DOutH", @"DOutL"]
                        itemSelectBlock:^(NSInteger index)
     {
         if (index == 0) {
             [[WTBLESDK sharedInstance] D2Cali:WTPortMode_AnalogInput];
         } else if (index == 1) {
             [[WTBLESDK sharedInstance] D2Cali:WTPortMode_DigitalInput];
         } else if (index == 2) {
             [[WTBLESDK sharedInstance] D2Cali:WTPortMode_HighLevelOutput];
         } else if (index == 3) {
             [[WTBLESDK sharedInstance] D2Cali:WTPortMode_LowLevelOutput];
         }
     }];
}

- (IBAction)onD3ButtonAction:(id)sender {
    [self showActionSheetAlertWithTitle:NSLocalizedString(@"select_port_mode", nil)
                           actionTitles:@[@"AIn", @"DIn", @"DOutH", @"DOutL"]
                        itemSelectBlock:^(NSInteger index)
     {
         if (index == 0) {
             [[WTBLESDK sharedInstance] D3Cali:WTPortMode_AnalogInput];
         } else if (index == 1) {
             [[WTBLESDK sharedInstance] D3Cali:WTPortMode_DigitalInput];
         } else if (index == 2) {
             [[WTBLESDK sharedInstance] D3Cali:WTPortMode_HighLevelOutput];
         } else if (index == 3) {
             [[WTBLESDK sharedInstance] D3Cali:WTPortMode_LowLevelOutput];
         }
     }];
}

- (IBAction)onVelocityButtonAction:(id)sender {
    [self showActionSheetAlertWithTitle:NSLocalizedString(@"select_output_rate", nil)
                           actionTitles:@[@"0.1Hz", @"0.2Hz", @"0.5Hz", @"1Hz", @"2Hz", @"5Hz", @"10Hz", @"20Hz", @"50Hz"]
                        itemSelectBlock:^(NSInteger index) {
                            if (index == 0) {
                                [[WTBLESDK sharedInstance] velocityCali:0.1];
                            } else if (index == 1) {
                                [[WTBLESDK sharedInstance] velocityCali:0.2];
                            } else if (index == 2) {
                                [[WTBLESDK sharedInstance] velocityCali:0.5];
                            } else if (index == 3) {
                                [[WTBLESDK sharedInstance] velocityCali:1];
                            } else if (index == 4) {
                                [[WTBLESDK sharedInstance] velocityCali:2];
                            } else if (index == 5) {
                                [[WTBLESDK sharedInstance] velocityCali:5];
                            } else if (index == 6) {
                                [[WTBLESDK sharedInstance] velocityCali:10];
                            } else if (index == 7) {
                                [[WTBLESDK sharedInstance] velocityCali:20];
                            } else if (index == 8) {
                                [[WTBLESDK sharedInstance] velocityCali:50];
                            }
    }];
}

- (IBAction)onSaveButtonAction:(id)sender {
    [[WTBLESDK sharedInstance] save];
}

- (IBAction)onResumeButtonAction:(id)sender {
    [[WTBLESDK sharedInstance] resume];
}

- (IBAction)onRenameButtonAction:(id)sender {
    [self showRenameAlert];
}

#pragma mark - UICollectionViewDelegate

- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    if (indexPath.item < self.itemDataList.count) {
        ItemDataModel *item= [self.itemDataList objectAtIndex:indexPath.item];
        self.selectDataType = item.dataType;
    }
    
    if (indexPath.item != 0) {
        UICollectionViewCell *cell = [collectionView cellForItemAtIndexPath:[NSIndexPath indexPathForItem:0 inSection:0]];
        cell.selected = NO;
    }
}

#pragma mark - UICollectionViewDataSource

- (NSInteger)collectionView:(UICollectionView *)collectionView
     numberOfItemsInSection:(NSInteger)section
{
    return self.itemDataList.count;
}

- (__kindof UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath
{
    HeaderItemCell *cell = [collectionView dequeueReusableCellWithReuseIdentifier:[self cellReuseIdentifier] forIndexPath:indexPath];
    if (self.isFirstTimeLoad && indexPath.item == 0) {
        cell.selected = YES;
        self.isFirstTimeLoad = NO;
    }
    if (indexPath.item < self.itemDataList.count) {
        ItemDataModel *item= [self.itemDataList objectAtIndex:indexPath.item];
        cell.titleLabel.text = item.title;
    }
    return cell;
}

#pragma mark - show alert

- (void)showActionSheetAlertWithTitle:(NSString *)title
                         actionTitles:(NSArray<NSString *> *)actionTitles
                      itemSelectBlock:(void(^)(NSInteger index))itemSelectBlock {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:title message:nil preferredStyle:UIAlertControllerStyleActionSheet];
    
    for (NSInteger i = 0; i < actionTitles.count; i++) {
        NSString *actionTitle = [actionTitles objectAtIndex:i];
        UIAlertAction *action = [UIAlertAction actionWithTitle:actionTitle
                                                         style:UIAlertActionStyleDefault
                                                       handler:^(UIAlertAction * _Nonnull action) {
                                                           if (itemSelectBlock) {
                                                               itemSelectBlock(i);
                                                           }
        }];
        [alertController addAction:action];
    }
    
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"cancel", nil)
                                                           style:UIAlertActionStyleCancel
                                                         handler:nil];
    [alertController addAction:cancelAction];
    [self presentViewController:alertController animated:YES completion:nil];
}

- (void)showRenameAlert {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"input_ble_device_name", nil) message:nil preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"cancel", nil) style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
        
    }];
    __weak typeof(self) weakSelf = self;
    NSString *currentName = self.peripheral.peripheral.name;
    UIAlertAction *doneAction = [UIAlertAction actionWithTitle:NSLocalizedString(@"done", nil) style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        __strong typeof(self) strongSelf = weakSelf;
        [strongSelf rename:alert.textFields.firstObject.text];
    }];
    [alert addAction:cancelAction];
    [alert addAction:doneAction];
    [alert addTextFieldWithConfigurationHandler:^(UITextField * _Nonnull textField) {
        textField.placeholder = currentName;
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(alertTextFieldDidChange:) name:UITextFieldTextDidChangeNotification object:textField];
    }];
    doneAction.enabled = NO;
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)rename:(NSString *)name {
     [[WTBLESDK sharedInstance] changeBLEDeviceName:name];
}

- (void)alertTextFieldDidChange:(NSNotification *)notification {
    UIAlertController *alert = (UIAlertController *)self.presentedViewController;
    if (alert) {
        UITextField *textfield = alert.textFields.firstObject;
        UIAlertAction *doneAction = alert.actions.lastObject;
        NSUInteger length = [textfield.text lengthOfBytesUsingEncoding:NSUTF8StringEncoding];
        doneAction.enabled = length > 0;
    }
}

- (void)dealWithUpdateBLENameInfo:(NSError *)error {
    if (error) {
        UIAlertController *alert = [UIAlertController alertControllerWithTitle:error.localizedDescription message:nil preferredStyle:UIAlertControllerStyleAlert];
        UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"done", nil) style:UIAlertActionStyleDefault handler:nil];
        [alert addAction:action];
        [self presentViewController:alert animated:YES completion:nil];
    } else {
        UIAlertController *alert = [UIAlertController alertControllerWithTitle:NSLocalizedString(@"change_name_success", nil) message:nil preferredStyle:UIAlertControllerStyleAlert];
        UIAlertAction *action = [UIAlertAction actionWithTitle:NSLocalizedString(@"done", nil) style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
            [self.navigationController popViewControllerAnimated:YES];
        }];
        [alert addAction:action];
        [self presentViewController:alert animated:YES completion:nil];
    }
}

@end
