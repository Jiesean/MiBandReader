# MiBandReader
读取小米手环步数、电量、控制震动


### 使用
1. 修改LeService里面的mTargetDeviceName为小米手环的设备名称。
2. 扫描，获得目标设备
3. 绑定
4. 连接，同时自动完成读取电量和步数
5. 点击相应按钮控制震动

### 版本

- 仅适用于小米手环1 非心率版，其他版本未经测试
- Android 5.0以上，Android 4.3系统需要手动修改Scan函数

### 更多信息

[Android BLE开发之玩转小米手环](http://www.jianshu.com/p/a274e17fc66a)