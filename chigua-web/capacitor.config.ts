import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.chigua.web',
  appName: 'ChiguaWeb',
  webDir: 'build',
  server: {
    androidScheme: 'https',
    allowNavigation: [
      '27.124.10.130',
      '*.rklzu.cc',
      '*.cqqvl.cc', 
      '*.uoatx.cc'
    ]
  },
  android: {
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: true
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: '#000000',
      showSpinner: false
    },
    StatusBar: {
      style: 'dark'
    }
  }
};

export default config;
