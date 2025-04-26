import { list } from "@vercel/blob"
import { NextResponse } from "next/server"

export async function GET() {
  try {
    // List all files in the research directory
    const { blobs } = await list({ prefix: "research/" })

    // In a real app, you would fetch additional metadata from your database
    // and combine it with the blob data

    return NextResponse.json({
      files: blobs.map((blob) => ({
        url: blob.url,
        pathname: blob.pathname,
        size: blob.size,
        uploadedAt: blob.uploadedAt,
      })),
    })
  } catch (error) {
    console.error("Error listing files:", error)
    return NextResponse.json({ error: "Error listing files" }, { status: 500 })
  }
}
