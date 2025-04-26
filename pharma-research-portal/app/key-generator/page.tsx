"use client"

import { useState } from "react"
import { EyeIcon, EyeOffIcon, CopyIcon, CheckIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useToast } from "@/components/ui/use-toast"

export default function KeyGeneratorPage() {
  const [password, setPassword] = useState("")
  const [encryptedKey, setEncryptedKey] = useState("")
  const [fileId, setFileId] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [copied, setCopied] = useState(false)
  const { toast } = useToast()

  const fetchEncryptedKey = async () => {
    if (!fileId || !password) {
      toast({
        title: "Missing fields",
        description: "Enter both File ID and Password",
        variant: "destructive",
      })
      return
    }

    try {
      const confirmDelete = confirm("Are you sure you want to fetch this key?")
      if (!confirmDelete) return

      // Get CSRF token
      const csrfResponse = await fetch("http://localhost:7002/api/csrf", {
        credentials: "include",
      })
      const csrfToken =
        csrfResponse.headers.get("X-CSRF-TOKEN") ||
        (await csrfResponse.json()).token

      // Get Auth token
      const userData = localStorage.getItem("user")
      const authToken = userData ? JSON.parse(userData).token : null
      if (!authToken) throw new Error("Authentication required")

      const res = await fetch(
        `http://localhost:7002/api/secure-files/getEncryptionKey?fileId=${fileId}&password=${password}`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "X-CSRF-TOKEN": csrfToken || "",
            "Content-Type": "application/json",
          },
          credentials: "include",
        }
      )


      if (!res.ok) {
        const responseText = await res.text()
        console.error("Error message:", responseText)
        throw new Error(
          `Key fetch failed with status: ${res.status}, ${responseText}`
        )
      }

      const key = await res.text()
      setEncryptedKey(key)

      toast({
        title: "Key Fetched",
        description: "Encrypted key fetched and auto-filled",
      })
    } catch (err) {
      console.error(err)
      toast({
        title: "Fetch Failed",
        description: "Could not fetch encrypted key",
        variant: "destructive",
      })
    }
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)

    toast({
      title: "Copied!",
      description: "Encrypted key copied to clipboard",
    })
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center px-4">
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <CardTitle className="text-2xl">Key Generator</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <Label>File ID</Label>
            <Input
              placeholder="Enter File ID"
              value={fileId}
              onChange={(e) => setFileId(e.target.value)}
            />
          </div>

          <div>
            <Label>Password</Label>
            <div className="relative">
              <Input
                type={showPassword ? "text" : "password"}
                placeholder="Enter Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <span
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-2 top-2.5 cursor-pointer text-gray-500"
              >
                {showPassword ? <EyeOffIcon size={18} /> : <EyeIcon size={18} />}
              </span>
            </div>
          </div>

          <Button onClick={fetchEncryptedKey} className="w-full">
            Fetch Decrypted Key
          </Button>

          <div>
            <Label>Encrypted Key</Label>
            <div className="relative">
              <Input value={encryptedKey} readOnly className="pr-10" />
              <span
                onClick={() => copyToClipboard(encryptedKey)}
                className="absolute right-2 top-2.5 cursor-pointer text-gray-500"
              >
                {copied ? <CheckIcon size={18} /> : <CopyIcon size={18} />}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
