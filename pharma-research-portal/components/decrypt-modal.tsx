"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { useToast } from "@/components/ui/use-toast"
import { Lock, Download } from "lucide-react"

interface DecryptModalProps {
  isOpen: boolean
  onClose: () => void
  onDecrypt: (key?: string) => void
  onDownloadEncrypted: () => void
  fileName: string
}

export function DecryptModal({ isOpen, onClose, onDecrypt, onDownloadEncrypted, fileName }: DecryptModalProps) {
  const [decryptionKey, setDecryptionKey] = useState("")
  const [isProcessing, setIsProcessing] = useState(false)
  const { toast } = useToast()

  const handleDecrypt = async () => {
    setIsProcessing(true)
    try {
      onDecrypt(decryptionKey.trim() || undefined)
    } catch (error) {
      toast({
        title: "Decryption failed",
        description: "Invalid decryption key or corrupted file",
        variant: "destructive",
      })
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Lock className="h-5 w-5 text-primary" />
            Encrypted File
          </DialogTitle>
          <DialogDescription>
            This file is encrypted. You can download it encrypted or provide a decryption key.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="file-name">File</Label>
            <Input id="file-name" value={fileName} readOnly disabled />
          </div>
          <div className="space-y-2">
            <Label htmlFor="decryption-key">Decryption Key (Optional)</Label>
            <Input
              id="decryption-key"
              type="password"
              placeholder="Enter custom decryption key if you have one"
              value={decryptionKey}
              onChange={(e) => setDecryptionKey(e.target.value)}
            />
            <p className="text-xs text-muted-foreground">Leave empty to use the system default key for decryption.</p>
          </div>
        </div>
        <DialogFooter className="flex flex-col sm:flex-row gap-2">
          <Button variant="outline" onClick={onDownloadEncrypted} className="sm:order-1 order-2">
            <Download className="h-4 w-4 mr-2" />
            Download Encrypted
          </Button>
          <Button onClick={handleDecrypt} disabled={isProcessing} className="sm:order-2 order-1">
            {isProcessing ? "Decrypting..." : "Decrypt & Download"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
