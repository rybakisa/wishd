import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { supabase } from '../lib/supabase';

type AuthMode = 'sign_in' | 'sign_up';

export function AuthScreen() {
  const [mode, setMode] = useState<AuthMode>('sign_in');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [magicLinkSent, setMagicLinkSent] = useState(false);

  const handleEmailAuth = async () => {
    if (!email) return;
    setLoading(true);
    try {
      if (mode === 'sign_in' && !password) {
        // Magic link
        const { error } = await supabase.auth.signInWithOtp({
          email,
          options: { shouldCreateUser: true },
        });
        if (error) throw error;
        setMagicLinkSent(true);
      } else if (mode === 'sign_up') {
        const { error } = await supabase.auth.signUp({ email, password });
        if (error) throw error;
        Alert.alert('Check your email', 'We sent a confirmation link to ' + email);
      } else {
        const { error } = await supabase.auth.signInWithPassword({ email, password });
        if (error) throw error;
      }
    } catch (err: any) {
      Alert.alert('Auth error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setLoading(true);
    try {
      const { error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: 'wishlistapp://auth-callback',
        },
      });
      if (error) throw error;
    } catch (err: any) {
      Alert.alert('Google sign-in error', err.message);
    } finally {
      setLoading(false);
    }
  };

  if (magicLinkSent) {
    return (
      <View style={styles.centered}>
        <Text style={styles.magicIcon}>📬</Text>
        <Text style={styles.magicTitle}>Check your email</Text>
        <Text style={styles.magicSubtitle}>
          We sent a magic link to{'\n'}<Text style={styles.emailBold}>{email}</Text>
        </Text>
        <TouchableOpacity onPress={() => setMagicLinkSent(false)} style={styles.backLink}>
          <Text style={styles.backLinkText}>Use a different email</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>🎁 Wishlist</Text>
        <Text style={styles.subtitle}>
          {mode === 'sign_in' ? 'Sign in to share your wishlist' : 'Create an account'}
        </Text>

        {/* Google Sign-In */}
        <TouchableOpacity
          style={styles.googleButton}
          onPress={handleGoogleSignIn}
          disabled={loading}
        >
          <Text style={styles.googleButtonText}>Continue with Google</Text>
        </TouchableOpacity>

        <View style={styles.divider}>
          <View style={styles.dividerLine} />
          <Text style={styles.dividerText}>or</Text>
          <View style={styles.dividerLine} />
        </View>

        {/* Email input */}
        <TextInput
          style={styles.input}
          placeholder="Email address"
          placeholderTextColor="#9ca3af"
          value={email}
          onChangeText={setEmail}
          keyboardType="email-address"
          autoCapitalize="none"
          autoComplete="email"
        />

        {/* Password (optional — shown for sign up or explicit sign in) */}
        {(mode === 'sign_up' || password.length > 0) && (
          <TextInput
            style={styles.input}
            placeholder={mode === 'sign_up' ? 'Password' : 'Password (or leave empty for magic link)'}
            placeholderTextColor="#9ca3af"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
            autoComplete="password"
          />
        )}

        {mode === 'sign_in' && password.length === 0 && (
          <Text style={styles.hint}>Leave password empty to receive a magic link</Text>
        )}

        <TouchableOpacity
          style={[styles.primaryButton, loading && styles.disabled]}
          onPress={handleEmailAuth}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.primaryButtonText}>
              {mode === 'sign_in'
                ? password.length > 0
                  ? 'Sign in'
                  : 'Send magic link'
                : 'Create account'}
            </Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => setMode(mode === 'sign_in' ? 'sign_up' : 'sign_in')}
          style={styles.switchMode}
        >
          <Text style={styles.switchModeText}>
            {mode === 'sign_in'
              ? "Don't have an account? Sign up"
              : 'Already have an account? Sign in'}
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flexGrow: 1, padding: 24, justifyContent: 'center', backgroundColor: '#f9fafb' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 32, backgroundColor: '#f9fafb' },
  title: { fontSize: 32, fontWeight: '800', color: '#111827', textAlign: 'center', marginBottom: 8 },
  subtitle: { fontSize: 16, color: '#6b7280', textAlign: 'center', marginBottom: 32 },
  googleButton: {
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  googleButtonText: { fontSize: 16, fontWeight: '600', color: '#374151' },
  divider: { flexDirection: 'row', alignItems: 'center', marginBottom: 20 },
  dividerLine: { flex: 1, height: 1, backgroundColor: '#e5e7eb' },
  dividerText: { marginHorizontal: 12, color: '#9ca3af', fontSize: 13 },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 16,
    color: '#111827',
    marginBottom: 12,
  },
  hint: { fontSize: 12, color: '#9ca3af', marginBottom: 12, marginLeft: 4 },
  primaryButton: {
    backgroundColor: '#6366f1',
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 4,
    marginBottom: 16,
  },
  disabled: { opacity: 0.6 },
  primaryButtonText: { fontSize: 16, fontWeight: '700', color: '#fff' },
  switchMode: { alignItems: 'center' },
  switchModeText: { color: '#6366f1', fontSize: 14, fontWeight: '500' },
  magicIcon: { fontSize: 56, marginBottom: 16 },
  magicTitle: { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 8 },
  magicSubtitle: { fontSize: 16, color: '#6b7280', textAlign: 'center', marginBottom: 24, lineHeight: 24 },
  emailBold: { fontWeight: '700', color: '#374151' },
  backLink: { padding: 8 },
  backLinkText: { color: '#6366f1', fontSize: 14, fontWeight: '500' },
});
