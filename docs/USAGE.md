# ParseCraft Usage Guide

This guide explains how to use ParseCraft from the web UI.

## Live URLs

- Frontend: https://d1qzyiqerket0h.cloudfront.net
- Backend: https://jjttw5tiex.us-east-1.awsapprunner.com

## How to Use

1. Open the app in your browser.
2. In **Analyze text or file**, do one of the following:
   - Paste document text in the text area, or
   - Upload a `.pdf`, `.docx`, or `.txt` file.
3. Choose a **Summary style**:
   - Formal
   - Gen Z / Casual
   - Bard / Herald
4. Click **Analyze**.
5. Review results in the right panel:
   - Detected tone
   - Tone explanation
   - Generated summary

## Input Rules

- If both pasted text and file are provided, pasted text is used.
- Supported file types: `.pdf`, `.docx`, `.txt`
- Frontend file-size validation: up to `10MB`
- Frontend pasted text limit: `50,000` characters

## API Behavior Notes

- Backend receives multipart form data.
- `style` is required (`formal`, `everyday`, `bard`).
- Backend may return `400`, `422`, or `502` when input or provider responses are invalid.

## Health Checks

- Frontend health: `/health`
- Backend health: `/api/v1/health`

## Quick curl example

```bash
curl -X POST "https://jjttw5tiex.us-east-1.awsapprunner.com/api/v1/documents/analyze" \
  -F "style=everyday" \
  -F "text=Paste your document text here"
```
