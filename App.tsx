import React, { useEffect, useState, useRef } from 'react';
import {
  View,
  Text,
  Button,
  Image,
  StyleSheet,
  Linking,
} from 'react-native';
import { Camera, useCameraDevice } from 'react-native-vision-camera';
import OpenCV from './src/NativeModules/OpenCV';

function App(): JSX.Element {
  const camera = useRef<Camera>(null);
  const devices: any = useCameraDevice('back');
  const [showCamera, setShowCamera] = useState(false);
  const [imageSource, setImageSource] = useState('');

  useEffect(() => {
    async function getPermission() {
      const permission = await Camera.requestCameraPermission();
      console.log('Camera permission status:', permission);
      // If permission is denied, open the device settings
      if (permission === 'denied') await Linking.openSettings();
    }
    getPermission();
  }, []);

  const capturePhoto = async (): Promise<void> => {
    if (camera.current !== null) {
      try {
        const currentCamera = camera.current;
        const photo: { path: string } = await currentCamera.takePhoto({});
        console.log(photo.path,"photo.path")
        OpenCV.removeGlare(photo.path, 
          (error: string) => {
            console.error('Error removing glare:', error);
          },
          (processedImageUri: string) => {
            setImageSource(`file://${processedImageUri}`);
            setShowCamera(false);
          }
        );
        setShowCamera(false);
      } catch (error) {
        console.error('Error capturing photo:', error);
      }
    }
  };

  const openCamera = () => {
    setShowCamera(true);
  };

  const closeCamera = () => {
    setShowCamera(false);
  };


  
  return (
    <View style={styles.container}>
      <Text style={styles.heading}>Capture the moment</Text>
      {showCamera ? (
        <>
          <Camera ref={camera} isActive={showCamera} photo={true} device={devices} />
          <View style={styles.container}>
          <View style={styles.buttonContainer}>
            <Button title="Take Picture"  onPress={capturePhoto}  color="#81b741"  />
            <Button title="Close Camera" onPress={closeCamera}  color="#81b741"/>
            </View>
          </View>
        </>
      ) : (
        <>
          {imageSource !== '' ? (
            <Image source={{ uri: imageSource  }} style={styles.image} />
          ) : null}
          <View style={styles.buttonContainer}>
            <Button title="Open Camera" onPress={openCamera} color="#81b741" />
          </View>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#372e47',
    justifyContent: 'center',
    alignItems: 'center',
  },
  heading: {
    color: '#fff',
    fontSize: 24,
    fontWeight: 'bold',
    fontStyle: 'italic',
    marginBottom: 20,
    padding:10
  },
  
  buttonContainer: {
    marginTop: 20,
    padding: 20,
    flexDirection: 'row',
    justifyContent: 'space-around',
    width: '100%',
  },
  image: {
    width: 200,
    height: 200,
  },
});

export default App;
