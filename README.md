# BackgroundCamera
BackgroundCamera is a camera that take a photo looks like on phone desktop.

实现原理，将Activity 主题设置为透明，因为使用相机拍照必须要进行预览，不要问我为什么，Android 规定，否则会报非法使用相机的错误，因此必须要进行相机预览，所以我们就要加载一个surfaceView,而它会出现一个黑框，因此我们必须要用它而且还要达到无预览，我们将其控件宽高设置为0.1dp即可，基本能达到看不到的效果，每次拍完照之后就将Activity销毁即可，这样就实现了后台的拍照效果。
