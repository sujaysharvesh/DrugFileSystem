"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  FileIcon,
  FileTextIcon,
  DownloadIcon,
  TrashIcon,
  SearchIcon,
  LockIcon,
  KeyIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useToast } from "@/components/ui/use-toast";
import { DecryptModal } from "@/components/decrypt-modal";
import { decryptFile } from "@/utils/encryption";

interface FileData {
  fileId: string;
  url: string;
  pathname: string;
  size: number;
  uploadedAt: Date;
  isEncrypted: boolean;
  originalType: string;
  originalName: string;
  title?: string;
  description?: string;
  userId?: string;
}

interface ApiError {
  message: string;
  status?: number;
}

export default function DashboardPage() {
  const [files, setFiles] = useState<FileData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [decryptModalOpen, setDecryptModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<FileData | null>(null);
  const { toast } = useToast();

  useEffect(() => {
    fetchFiles();
  }, []);

  const fetchFiles = async (): Promise<void> => {
    try {
      setIsLoading(true);

      // 1. First fetch CSRF token
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

      const csrfTokenFromHeader = csrfResponse.headers.get("X-CSRF-TOKEN");
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

      const response = await fetch("http://localhost:7002/api/user/files", {
        method: "GET",
        credentials: "include",
        headers: {
          Authorization: `Bearer ${token}`,
          "X-CSRF-TOKEN": csrfToken,
          Accept: "application/json",
        },
      });

      if (!response.ok) {
        const errorData: ApiError = await response.json().catch(() => ({
          message: "Failed to fetch files",
        }));
        throw new Error(
          errorData.message || `Request failed with status ${response.status}`
        );
      }

      const data = await response.json();

      // 3. Transform API response to FileData format
      const parsedFiles: FileData[] = data.map((file: any) => ({
        fileId: file.fileId,
        url: `https://your-s3-bucket-url/${file.s3Key}`,
        pathname: file.s3Key,
        size: file.fileSize || 0,
        uploadedAt: new Date(file.uploadTime),
        isEncrypted: !!file.encryptionKey,
        originalName: file.filename,
        originalType: file.contentType || "application/octet-stream",
        title: file.title,
        description: file.description,
        userId: file.userId,
      }));

      setFiles(parsedFiles);
    } catch (error) {
      console.error("Error fetching files:", error);

      toast({
        title: "Error",
        description:
          error instanceof Error ? error.message : "Failed to load files",
        variant: "destructive",
      });

      // Optional: Redirect to login if unauthorized
      if (error instanceof Error && error.message.includes("401")) {
        window.location.href = "/login";
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleDownload = async (file: FileData) => {
    if (file.isEncrypted) {
      setSelectedFile(file);
      setDecryptModalOpen(true);
    } else {
      try {
        // Get CSRF token first
        const csrfResponse = await fetch("http://localhost:7002/api/csrf", {
          credentials: "include",
        });
        const csrfToken = csrfResponse.headers.get("X-CSRF-TOKEN") || 
                          (await csrfResponse.json()).token;

        // Get auth token
        const userData = localStorage.getItem("user");
        const authToken = userData ? JSON.parse(userData).token : null;

        if (!authToken) {
          throw new Error("Authentication required");
        }

        // Create download URL with fileId
        const downloadUrl = `http://localhost:7002/api/secure-files/download?fileId=${file.fileId}`;

        // Use fetch to get the file with auth headers
        const response = await fetch(downloadUrl, {
          method: "GET",
          headers: {
            "Authorization": `Bearer ${authToken}`,
            "X-CSRF-TOKEN": csrfToken || "",
          },
          credentials: "include",
        });
        
        if (!response.ok) {
          throw new Error(`Download failed with status: ${response.status}`);
        }
        
        // Get the file as a blob
        const blob = await response.blob();
        
        // Create object URL and trigger download
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = file.originalName || file.pathname.split("/").pop() || "file";
        document.body.appendChild(a);
        a.click();
        
        // Clean up
        setTimeout(() => {
          URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        toast({
          title: "Download successful",
          description: "File downloaded successfully",
        });
      } catch (error) {
        console.error("Download error:", error);
        toast({
          title: "Download failed",
          description: error instanceof Error ? error.message : "Failed to download file",
          variant: "destructive",
        });
      }
    }
  };

  const handleDownloadEncrypted = async () => {
    if (!selectedFile) return;

    try {
      // Get CSRF token
      const csrfResponse = await fetch("http://localhost:7002/api/csrf", {
        credentials: "include",
      });
      const csrfToken = csrfResponse.headers.get("X-CSRF-TOKEN") || 
                        (await csrfResponse.json()).token;

      // Get auth token
      const userData = localStorage.getItem("user");
      const authToken = userData ? JSON.parse(userData).token : null;

      if (!authToken) {
        throw new Error("Authentication required");
      }

      // Use consistent port and proper URL format
      const downloadUrl = `http://localhost:7002/api/secure-files/download?fileId=${selectedFile.fileId}`;

      // Use fetch to get the file with auth headers
      const response = await fetch(downloadUrl, {
        method: "GET",
        headers: {
          "Authorization": `Bearer ${authToken}`,
          "X-CSRF-TOKEN": csrfToken || "",
        },
        credentials: "include",
      });
      
      if (!response.ok) {
        throw new Error(`Download failed with status: ${response.status}`);
      }
      
      // Get the file as a blob
      const blob = await response.blob();
      
      // Create object URL and trigger download
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${selectedFile.originalName || selectedFile.pathname.split("/").pop() || "file"}.enc`;
      document.body.appendChild(a);
      a.click();
      
      // Clean up
      setTimeout(() => {
        URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }, 100);

      setDecryptModalOpen(false);
      toast({
        title: "Download started",
        description: "Encrypted file is being downloaded",
      });
    } catch (error) {
      console.error("Download error:", error);
      toast({
        title: "Download failed",
        description: error instanceof Error ? error.message : "Failed to download file",
        variant: "destructive",
      });
    }
  };

  const handleDecrypt = async (customKey?: string) => {
    if (!selectedFile) return;

    try {
      // Get CSRF token
      const csrfResponse = await fetch("http://localhost:7002/api/csrf", {
        credentials: "include",
      });
      const csrfToken = csrfResponse.headers.get("X-CSRF-TOKEN") || 
                        (await csrfResponse.json()).token;

      // Get auth token
      const userData = localStorage.getItem("user");
      const authToken = userData ? JSON.parse(userData).token : null;

      if (!authToken) {
        throw new Error("Authentication required");
      }

      // Use consistent port and proper URL creation
      let downloadUrl = `http://localhost:7002/api/secure-files/download?fileId=${selectedFile.fileId}`;
      if (customKey) {
        downloadUrl += `&decryptionKey=${encodeURIComponent(customKey)}`;
      }

      // Fetch the decrypted file
      const response = await fetch(downloadUrl, {
        method: "GET",
        headers: {
          "Authorization": `Bearer ${authToken}`,
          "X-CSRF-TOKEN": csrfToken || "",
        },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Decryption failed with status: ${response.status}`);
      }

      // Create download link
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = selectedFile.originalName || selectedFile.pathname.split("/").pop() || "file";
      document.body.appendChild(a);
      a.click();

      // Clean up
      setTimeout(() => {
        URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }, 100);

      setDecryptModalOpen(false);
      toast({
        title: "Download complete",
        description: "File has been decrypted and downloaded",
      });
    } catch (error) {
      console.error("Decryption error:", error);
      toast({
        title: "Decryption failed",
        description: error instanceof Error ? error.message : "Invalid key or file error",
        variant: "destructive",
      });
    }
  };

  const handleDelete = async (fileId: string) => {
    try {
      // Confirm deletion first
      if (!confirm("Are you sure you want to delete this file? This action cannot be undone.")) {
        return;
      }

      // Get CSRF token first
      const csrfResponse = await fetch("http://localhost:7002/api/csrf", {
        credentials: "include",
      });
      const csrfToken = csrfResponse.headers.get("X-CSRF-TOKEN") ||
                       (await csrfResponse.json()).token;

      // Get auth token
      const userData = localStorage.getItem("user");
      const authToken = userData ? JSON.parse(userData).token : null;

      if (!authToken) {
        throw new Error("Authentication required");
      }

      // Delete the file
      const response = await fetch(`http://localhost:7002/api/secure-files/?fileId=${fileId}`, {
        method: "DELETE",
        headers: {
          "Authorization": `Bearer ${authToken}`,
          "X-CSRF-TOKEN": csrfToken || "",
          "Content-Type": "application/json"
        },
        credentials: "include",
      });

      if (!response.ok) {
        const responseTest = response.text();
        console.log("Error message:", responseTest);
        throw new Error(`File deletion failed with status: ${response.status}`);
      }

      // Update the file list
      setFiles(files.filter(file => file.fileId !== fileId));

      toast({
        title: "File deleted",
        description: "The file has been successfully deleted.",
      });
    } catch (error) {
      console.error("Delete error:", error);
      toast({
        title: "Delete failed",
        description: error instanceof Error ? error.message : "Failed to delete file",
        variant: "destructive",
      });
    }
  };

  const handleGenerateKey = () => {
    // Generate a secure random key
    const array = new Uint8Array(16);
    window.crypto.getRandomValues(array);
    const key = Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
    
    // Copy to clipboard
    navigator.clipboard.writeText(key).then(
      () => {
        toast({
          title: "Encryption key generated",
          description: "The key has been copied to your clipboard.",
        });
      },
      () => {
        toast({
          title: "Key generated",
          description: `Your encryption key: ${key}`,
        });
      }
    );
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return (
      Number.parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i]
    );
  };

  const formatDate = (date: Date) => {
    return new Date(date).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const filteredFiles = files.filter(
    (file) =>
      file.pathname.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (file.title &&
        file.title.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (file.description &&
        file.description.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const getFileIcon = (pathname: string, isEncrypted?: boolean) => {
    if (isEncrypted) {
      return <LockIcon className="h-5 w-5 text-amber-500" />;
    }

    const extension = pathname.split(".").pop()?.toLowerCase();

    if (extension === "pdf") {
      return <FileTextIcon className="h-5 w-5 text-red-500" />;
    } else if (["doc", "docx"].includes(extension || "")) {
      return <FileTextIcon className="h-5 w-5 text-blue-500" />;
    } else if (["xls", "xlsx", "csv"].includes(extension || "")) {
      return <FileTextIcon className="h-5 w-5 text-green-500" />;
    } else {
      return <FileIcon className="h-5 w-5 text-gray-500" />;
    }
  };

  return (
    <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            My Research Files
          </h1>
          <p className="text-muted-foreground mt-1">
            Manage and access your uploaded research documents
          </p>
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
          <Link href={"/key-generator"}>
          <Button onClick={handleGenerateKey} variant="outline">
            <KeyIcon className="h-4 w-4 mr-2" />
            Generate Key
          </Button>
          </Link>
          <Link href="/dashboard/upload">
            <Button>Upload New</Button>
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
                {searchQuery
                  ? "Try a different search term"
                  : "Upload your first research file to get started"}
              </p>
              {!searchQuery && (
                <Link href="/dashboard/upload">
                  <Button>Upload File</Button>
                </Link>
              )}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>File Name</TableHead>
                  <TableHead className="hidden md:table-cell">Size</TableHead>
                  <TableHead className="hidden md:table-cell">
                    Uploaded
                  </TableHead>
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
                    </TableCell>
                    <TableCell className="hidden md:table-cell">
                      {formatFileSize(file.size)}
                    </TableCell>
                    <TableCell className="hidden md:table-cell">
                      {formatDate(file.uploadedAt)}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleDownload(file)}
                        >
                          <DownloadIcon className="h-4 w-4" />
                          <span className="sr-only">Download</span>
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleDelete(file.fileId)}
                        >
                          <TrashIcon className="h-4 w-4" />
                          <span className="sr-only">Delete</span>
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {selectedFile && (
        <DecryptModal
          isOpen={decryptModalOpen}
          onClose={() => setDecryptModalOpen(false)}
          onDecrypt={handleDecrypt}
          onDownloadEncrypted={handleDownloadEncrypted}
          fileName={
            selectedFile.title || selectedFile.pathname.split("/").pop() || ""
          }
        />
      )}
    </div>
  );
}