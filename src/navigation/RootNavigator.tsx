import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { HomeScreen } from '../screens/HomeScreen';
import { AuthScreen } from '../screens/AuthScreen';
import { CreateWishlistScreen } from '../screens/CreateWishlistScreen';
import { WishlistDetailScreen } from '../screens/WishlistDetailScreen';
import { AddItemScreen } from '../screens/AddItemScreen';
import { RootStackParamList } from '../types';
import { useAuth } from '../hooks/useAuth';

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  const { isAuthenticated } = useAuth();

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: '#fff' },
          headerTintColor: '#111827',
          headerTitleStyle: { fontWeight: '700' },
          contentStyle: { backgroundColor: '#f9fafb' },
        }}
      >
        {/* Home is always accessible (progressive auth) */}
        <Stack.Screen
          name="Home"
          component={HomeScreen}
          options={{ title: 'My Wishlists' }}
        />
        <Stack.Screen
          name="WishlistDetail"
          component={WishlistDetailScreen}
          options={{ title: 'Wishlist' }}
        />
        <Stack.Screen
          name="CreateWishlist"
          component={CreateWishlistScreen}
          options={{ title: 'New Wishlist', presentation: 'modal' }}
        />
        <Stack.Screen
          name="AddItem"
          component={AddItemScreen}
          options={{ title: 'Add Item', presentation: 'modal' }}
        />
        <Stack.Screen
          name="Auth"
          component={AuthScreen}
          options={{
            title: isAuthenticated ? 'Account' : 'Sign In',
            presentation: 'modal',
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
