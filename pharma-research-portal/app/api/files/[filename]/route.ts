import { del } from "@vercel/blob"
import { NextResponse } from "next/server"

export async function DELETE(request: Request, { params }: { params: { filename: string } }) {
  try {
    const filename = params.filename

    // Delete the file from Vercel Blob
    await del(`research/${filename}`)

    // In a real app, you would also delete metadata from your database

    return NextResponse.json({ success: true })
  } catch (error) {
    console.error("Error deleting file:", error)
    return NextResponse.json({ error: "Error deleting file" }, { status: 500 })
  }
}
