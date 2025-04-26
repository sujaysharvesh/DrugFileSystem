// System-defined encryption key (in a real app, this would be stored securely)
const SYSTEM_ENCRYPTION_KEY = "pharma-research-portal-system-key-2023"

// Function to generate a key from a password
export async function deriveKey(password: string = SYSTEM_ENCRYPTION_KEY): Promise<CryptoKey> {
  const encoder = new TextEncoder()
  const passwordData = encoder.encode(password)

  // Create a key from the password
  const passwordKey = await crypto.subtle.importKey("raw", passwordData, { name: "PBKDF2" }, false, ["deriveKey"])

  // Salt should be stored with the file in a real application
  // For simplicity, we're using a fixed salt here
  const salt = encoder.encode("pharmaceutical-research-portal-salt")

  // Derive a key for AES-GCM
  return crypto.subtle.deriveKey(
    {
      name: "PBKDF2",
      salt,
      iterations: 100000,
      hash: "SHA-256",
    },
    passwordKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  )
}

// Function to encrypt a file
export async function encryptFile(file: File, customKey?: string): Promise<{ encryptedFile: Blob; iv: Uint8Array }> {
  const key = await deriveKey(customKey)
  const iv = crypto.getRandomValues(new Uint8Array(12)) // Initialization vector

  // Read the file as an ArrayBuffer
  const fileBuffer = await file.arrayBuffer()

  // Encrypt the file
  const encryptedBuffer = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, fileBuffer)

  // Create a new blob that contains the IV followed by the encrypted data
  const encryptedBlob = new Blob([iv, new Uint8Array(encryptedBuffer)], { type: "application/encrypted" })

  return { encryptedFile: encryptedBlob, iv }
}

// Function to decrypt a file
export async function decryptFile(
  encryptedBlob: Blob,
  originalFileName: string,
  originalType: string,
  customKey?: string,
): Promise<File> {
  const key = await deriveKey(customKey)

  // Read the encrypted blob as an ArrayBuffer
  const encryptedBuffer = await encryptedBlob.arrayBuffer()

  // Extract the IV (first 12 bytes)
  const iv = new Uint8Array(encryptedBuffer.slice(0, 12))

  // Extract the encrypted data (everything after the IV)
  const encryptedData = encryptedBuffer.slice(12)

  try {
    // Decrypt the data
    const decryptedBuffer = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, key, encryptedData)

    // Create a new file with the decrypted data
    return new File([decryptedBuffer], originalFileName, { type: originalType })
  } catch (error) {
    console.error("Decryption failed:", error)
    throw new Error("Decryption failed. Invalid key or corrupted file.")
  }
}
