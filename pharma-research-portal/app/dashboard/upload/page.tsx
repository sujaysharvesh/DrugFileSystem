"use client"

import type React from "react"

import { useState, useRef } from "react"
import { useRouter } from "next/navigation"
import { Upload, FileText, X, AlertCircle, Lock, Unlock } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Checkbox } from "@/components/ui/checkbox"
import { useToast } from "@/components/ui/use-toast"
import { put } from "@vercel/blob"
import { encryptFile } from "@/utils/encryption"

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [isEncrypted, setIsEncrypted] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const router = useRouter()
  const { toast } = useToast()

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0])
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setFile(e.dataTransfer.files[0])
    }
  }

  const handleRemoveFile = () => {
    setFile(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
  
    if (!file) {
      toast({
        title: "No file selected",
        description: "Please select a file to upload",
        variant: "destructive",
      })
      return
    }
  
    try {
      setIsUploading(true)
  
      let fileToUpload = file
      const originalType = file.type
      const originalName = file.name
  
      if (isEncrypted) {
        const { encryptedFile } = await encryptFile(file)
        fileToUpload = new File([encryptedFile], file.name, { type: "application/encrypted" })
      }
  
      const formData = new FormData()
      formData.append("file", fileToUpload)
      formData.append("title", title || file.name)
      formData.append("description", description)
      const csrfResponse = await fetch("http://localhost:7002/api/csrf", {
        method: "GET",
        credentials: "include", // Important for cookies
        headers: {
          Accept: "application/json",
        },
      });

      if (!csrfResponse.ok) {
        throw new Error(
          `CSRF token fetch failed with status ${csrfResponse.status}`
        );
      }

      const csrfTokenFromHeader = csrfResponse.headers.get("X-XSRF-TOKEN");
      const { token: csrfTokenFromBody } = await csrfResponse.json();

      // Use header token first, fallback to body token
      const csrfToken = csrfTokenFromHeader || csrfTokenFromBody;

      if (!csrfToken) {
        throw new Error("No CSRF token received");
      }

      // 2. Now fetch files with proper authorization
      const userData = localStorage.getItem("user");

      if (!userData) {
        throw new Error("No user session found");
      }

      const { token } = JSON.parse(userData);

      if (!token) {
        throw new Error("No authentication token found");
      }
    
      const response = await fetch("http://localhost:7002/api/secure-files/upload", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "X-XSRF-TOKEN": csrfToken,
          Accept: "application/json",
        },
        body: formData,
      })
  
      if (!response.ok) {
        const errorText = await response.text();
        console.error("Upload failed:", errorText);
        throw new Error("Upload failed");
      }
      
  
      toast({
        title: "Upload successful",
        description: isEncrypted
          ? "Your encrypted research file has been uploaded"
          : "Your research file has been uploaded",
      })
  
      router.push("/dashboard")
    } catch (error) {
      console.error("Upload error:", error)
      toast({
        title: "Upload failed",
        description: "There was an error uploading your file. Please try again.",
        variant: "destructive",
      })
    } finally {
      setIsUploading(false)
      setUploadProgress(0)
    }
  }
  

  const allowedFileTypes = [
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/csv",
    "application/zip",
    "application/x-zip-compressed",
  ]

  const isValidFileType = file ? allowedFileTypes.includes(file.type) : true

  return (
    <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-3xl font-bold tracking-tight mb-6">Upload Research File</h1>

        <form onSubmit={handleSubmit}>
          <Card>
            <CardHeader>
              <CardTitle>File Upload</CardTitle>
              <CardDescription>Upload your research documents, data, or reports</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div
                className={`border-2 border-dashed rounded-lg p-6 text-center ${
                  file ? "border-primary bg-primary/5" : "border-muted-foreground/20"
                }`}
                onDragOver={handleDragOver}
                onDrop={handleDrop}
              >
                {!file ? (
                  <div className="flex flex-col items-center justify-center py-4">
                    <Upload className="h-10 w-10 text-muted-foreground mb-2" />
                    <p className="mb-2 text-sm font-semibold">Drag and drop your file here or click to browse</p>
                    <p className="text-xs text-muted-foreground mb-4">
                      Supported formats: PDF, DOC, DOCX, XLS, XLSX, CSV, ZIP
                    </p>
                    <Button type="button" variant="secondary" onClick={() => fileInputRef.current?.click()}>
                      Browse Files
                    </Button>
                    <input
                      ref={fileInputRef}
                      type="file"
                      className="hidden"
                      accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.zip"
                      onChange={handleFileChange}
                    />
                  </div>
                ) : (
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <FileText className="h-8 w-8 text-primary" />
                      <div className="text-left">
                        <p className="text-sm font-medium truncate max-w-[200px] md:max-w-[300px]">{file.name}</p>
                        <p className="text-xs text-muted-foreground">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                      </div>
                    </div>
                    <Button type="button" variant="ghost" size="icon" onClick={handleRemoveFile}>
                      <X className="h-4 w-4" />
                      <span className="sr-only">Remove file</span>
                    </Button>
                  </div>
                )}
              </div>

              {file && !isValidFileType && (
                <div className="flex items-center gap-2 text-destructive text-sm">
                  <AlertCircle className="h-4 w-4" />
                  <span>Invalid file type. Please upload a supported format.</span>
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="title">Title</Label>
                <Input
                  id="title"
                  placeholder="Research title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  placeholder="Brief description of the research file"
                  rows={3}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>
              {isEncrypted && (
                <p className="text-sm text-muted-foreground">
                  Your file will be automatically encrypted before upload. You can optionally provide a custom
                  decryption key when downloading.
                </p>
              )}
            </CardContent>
            <CardFooter className="flex flex-col space-y-2">
              {isUploading && (
                <div className="w-full mb-2">
                  <div className="flex justify-between text-xs mb-1">
                    <span>Uploading...</span>
                    <span>{uploadProgress}%</span>
                  </div>
                  <div className="w-full bg-muted rounded-full h-2">
                    <div className="bg-primary h-2 rounded-full" style={{ width: `${uploadProgress}%` }}></div>
                  </div>
                </div>
              )}
              <Button type="submit" className="w-full" disabled={isUploading || !file || !isValidFileType}>
                {isUploading ? "Uploading..." : "Upload File"}
              </Button>
            </CardFooter>
          </Card>
        </form>
      </div>
    </div>
  )
}
