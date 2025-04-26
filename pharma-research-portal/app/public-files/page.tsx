"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { FileIcon, FileTextIcon, DownloadIcon, SearchIcon, LockIcon, HomeIcon } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useToast } from "@/components/ui/use-toast"
import { DecryptModal } from "@/components/decrypt-modal"
import { decryptFile } from "@/utils/encryption"

interface FileData {
  url: string
  pathname: string
  size: number
  uploadedAt: Date
  isEncrypted?: boolean
  originalType?: string
  originalName?: string
  title?: string
  description?: string
}

export default function PublicFilesPage() {
  const [files, setFiles] = useState<FileData[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [decryptModalOpen, setDecryptModalOpen] = useState(false)
  const [selectedFile, setSelectedFile] = useState<FileData | null>(null)
  const { toast } = useToast()

  useEffect(() => {
    fetchFiles()
  }, [])

  const fetchFiles = async () => {
    try {
      setIsLoading(true)
      // In a real app, you would fetch files from your API
      // For demo purposes, we'll use local storage to simulate stored files
      const storedFiles = localStorage.getItem("uploadedFiles")

      if (storedFiles) {
        const parsedFiles = JSON.parse(storedFiles)
        setFiles(
          parsedFiles.map((file: any) => ({
            ...file,
            uploadedAt: new Date(file.uploadedAt),
          })),
        )
      }
    } catch (error) {
      console.error("Error fetching files:", error)
      toast({
        title: "Error",
        description: "Failed to load files. Please try again.",
        variant: "destructive",
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handleDownload = async (file: FileData) => {
    if (file.isEncrypted) {
      setSelectedFile(file)
      setDecryptModalOpen(true)
    } else {
      // Regular download for non-encrypted files
      window.open(file.url, "_blank")
    }
  }

  const handleDecrypt = async (decryptionKey: string) => {
    if (!selectedFile) return

    try {
      // Fetch the encrypted file
      const response = await fetch(selectedFile.url)
      const encryptedBlob = await response.blob()

      // Decrypt the file
      const decryptedFile = await decryptFile(
        encryptedBlob,
        decryptionKey,
        selectedFile.originalName || selectedFile.pathname.split("/").pop() || "file",
        selectedFile.originalType || "application/octet-stream",
      )

      // Create a download link for the decrypted file
      const downloadUrl = URL.createObjectURL(decryptedFile)
      const a = document.createElement("a")
      a.href = downloadUrl
      a.download = decryptedFile.name
      document.body.appendChild(a)
      a.click()

      // Clean up
      setTimeout(() => {
        URL.revokeObjectURL(downloadUrl)
        document.body.removeChild(a)
      }, 100)

      setDecryptModalOpen(false)

      toast({
        title: "Decryption successful",
        description: "Your file has been decrypted and downloaded",
      })
    } catch (error) {
      console.error("Decryption error:", error)
      toast({
        title: "Decryption failed",
        description: "Invalid decryption key or corrupted file",
        variant: "destructive",
      })
    }
  }

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes"
    const k = 1024
    const sizes = ["Bytes", "KB", "MB", "GB"]
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i]
  }

  const formatDate = (date: Date) => {
    return new Date(date).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    })
  }

  const filteredFiles = files.filter(
    (file) =>
      file.pathname.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (file.title && file.title.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (file.description && file.description.toLowerCase().includes(searchQuery.toLowerCase())),
  )

  const getFileIcon = (pathname: string, isEncrypted?: boolean) => {
    if (isEncrypted) {
      return <LockIcon className="h-5 w-5 text-amber-500" />
    }

    const extension = pathname.split(".").pop()?.toLowerCase()

    if (extension === "pdf") {
      return <FileTextIcon className="h-5 w-5 text-red-500" />
    } else if (["doc", "docx"].includes(extension || "")) {
      return <FileTextIcon className="h-5 w-5 text-blue-500" />
    } else if (["xls", "xlsx", "csv"].includes(extension || "")) {
      return <FileTextIcon className="h-5 w-5 text-green-500" />
    } else {
      return <FileIcon className="h-5 w-5 text-gray-500" />
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="bg-primary text-primary-foreground py-6">
        <div className="container flex justify-between items-center">
          <h1 className="text-2xl font-bold">Pharma Research Portal</h1>
          <div className="flex gap-4">
            <Link href="/">
              <Button variant="secondary" size="sm" className="flex items-center gap-2">
                <HomeIcon className="h-4 w-4" />
                Home
              </Button>
            </Link>
            <Link href="/login">
              <Button variant="secondary" size="sm">
                Login
              </Button>
            </Link>
          </div>
        </div>
      </header>

      <div className="container py-8">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Public Research Files</h1>
            <p className="text-muted-foreground mt-1">Browse and download available pharmaceutical research files</p>
          </div>
          <div className="flex gap-4 w-full md:w-auto">
            <div className="relative w-full md:w-64">
              <SearchIcon className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                type="search"
                placeholder="Search files..."
                className="pl-8 w-full"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <Link href="/key-generator">
              <Button variant="outline">Key Generator</Button>
            </Link>
          </div>
        </div>

        <Card>
          <CardContent className="p-0">
            {isLoading ? (
              <div className="flex justify-center items-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
              </div>
            ) : filteredFiles.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 p-4 text-center">
                <FileIcon className="h-12 w-12 text-muted-foreground mb-4" />
                <h3 className="text-lg font-medium">No files found</h3>
                <p className="text-muted-foreground mt-1 mb-4">
                  {searchQuery ? "Try a different search term" : "No research files are currently available"}
                </p>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>File Name</TableHead>
                    <TableHead className="hidden md:table-cell">Size</TableHead>
                    <TableHead className="hidden md:table-cell">Uploaded</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredFiles.map((file) => (
                    <TableRow key={file.pathname}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {getFileIcon(file.pathname, file.isEncrypted)}
                          <span className="font-medium truncate max-w-[200px] md:max-w-[300px]">
                            {file.title || file.pathname.split("/").pop()}
                          </span>
                          {file.isEncrypted && (
                            <span className="text-xs bg-amber-100 text-amber-800 px-2 py-0.5 rounded-full">
                              Encrypted
                            </span>
                          )}
                        </div>
                        {file.description && (
                          <p className="text-sm text-muted-foreground mt-1 truncate max-w-[300px] md:max-w-[400px]">
                            {file.description}
                          </p>
                        )}
                      </TableCell>
                      <TableCell className="hidden md:table-cell">{formatFileSize(file.size)}</TableCell>
                      <TableCell className="hidden md:table-cell">{formatDate(file.uploadedAt)}</TableCell>
                      <TableCell className="text-right">
                        <Button variant="ghost" size="icon" onClick={() => handleDownload(file)}>
                          <DownloadIcon className="h-4 w-4" />
                          <span className="sr-only">Download</span>
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>

      {selectedFile && (
        <DecryptModal
          isOpen={decryptModalOpen}
          onClose={() => setDecryptModalOpen(false)}
          onDecrypt={handleDecrypt}
          fileName={selectedFile.title || selectedFile.pathname.split("/").pop() || ""}
        />
      )}
    </div>
  )
}

