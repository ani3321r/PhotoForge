# PhotoForge

A full-stack photo management application with AI-powered image transformations. PhotoForge lets users upload, organize, and enhance their photos using a rich suite of AI operations backed by ImageKit's CDN — all wrapped in a modern, responsive web interface.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
  - [High-Level Overview](#high-level-overview)
  - [Backend Architecture](#backend-architecture)
  - [Frontend Architecture](#frontend-architecture)
  - [Data Model](#data-model)
  - [Entity Relationships](#entity-relationships)
- [REST API Reference](#rest-api-reference)
  - [Authentication](#authentication-apiauthroutes)
  - [Photos](#photos-apiphotos)
  - [AI Transforms](#ai-transforms-apiphotosidag)
  - [Albums](#albums-apialbums)
  - [Library](#library-apilibrary)
- [Security Design](#security-design)
  - [JWT Strategy](#jwt-strategy)
  - [Refresh Token Rotation](#refresh-token-rotation)
- [AI Transformation Engine](#ai-transformation-engine)
  - [Supported Operations](#supported-operations)
  - [Preview vs Apply Flow](#preview-vs-apply-flow)
- [Photo Lifecycle](#photo-lifecycle)
- [ImageKit CDN Integration](#imagekit-cdn-integration)
- [Frontend Deep Dive](#frontend-deep-dive)
  - [Routing](#routing-nextjs-app-router)
  - [State Management](#state-management)
  - [Component Architecture](#component-architecture)
  - [API Layer](#api-layer)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Setup](#environment-setup)
  - [Running the App](#running-the-app)
- [Configuration Reference](#configuration-reference)

---

## Overview

PhotoForge is a production-grade photo management platform built as a monorepo with a decoupled Spring Boot backend and a Next.js frontend. It models the core workflows of a personal photo library: uploading images, organizing them into albums, archiving or trashing photos, and applying AI transformations like background removal, generative fill, and smart cropping — all processed through the ImageKit CDN.

The project showcases a real-world full-stack architecture with stateless JWT authentication, refresh token rotation, service-layer ownership enforcement, CDN-side AI processing with server-side polling, and a reactive frontend built on TanStack Query and Zustand.

---

## Features

**Photo Management**
- Upload photos (JPEG, PNG, WebP, GIF, HEIC/HEIF, up to 20 MB per file)
- Paginated photo library with infinite scroll ("Load More")
- Archive, trash, restore, and permanently delete photos
- Bulk select operations across the entire library
- Import photos already stored in your ImageKit account

**AI Transformations** (powered by ImageKit AI)
- Background removal and drop shadow
- Generative background change with custom prompt
- Generative fill with custom dimensions and optional prompt
- Smart crop (dimension-aware, focus-auto)
- Object-aware crop by named subject
- Portrait retouch
- AI upscale
- Free-form generative edit via text prompt
- Live preview before committing any transformation
- Transformed images saved as derived copies linked to their originals

**Album Organization**
- Create and manage albums
- Set custom cover photos
- Add / remove photos from albums
- Auto-assigned cover photo on first addition

**Library**
- Storage usage widget (bytes used / total photo count)
- Browse and import assets directly from your ImageKit folder

**Authentication**
- Email / password registration and login
- Stateless JWT access tokens (15-minute expiry)
- Rotating opaque refresh tokens (7-day expiry), persisted in the database
- Secure logout with server-side token revocation
- Persistent auth state via `localStorage` with rehydration guard

---

## Tech Stack

### Backend

| Layer | Technology |
|---|---|
| Framework | Spring Boot **4.1.1** |
| Language | Java **17** |
| Web | Spring MVC (`spring-boot-starter-webmvc`) |
| Security | Spring Security + JJWT **0.12.7** (HS256) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL **16** |
| Image CDN | ImageKit Java SDK **3.6.0** |
| Validation | Jakarta Bean Validation 3.1.1 |
| Boilerplate | Lombok |
| Build | Maven |

### Frontend

| Layer | Technology |
|---|---|
| Framework | Next.js **16.3.6** (App Router) |
| Language | TypeScript **5** |
| UI | React **19.2.8** |
| Styling | Tailwind CSS **v4** + shadcn/ui + Radix Primitives |
| Server State | TanStack React Query **v5** |
| Client State | Zustand **v5** |
| Forms | React Hook Form **v7** + Zod **v4** |
| Notifications | Sonner |
| Icons | Lucide React + Remix Icons |
| Theming | next-themes (dark / light) |
| Charts | Recharts |
| Infrastructure | Docker (PostgreSQL via `docker-compose.yml`) |

---

## Architecture

### High-Level Overview

```
┌──────────────────────────────────────────────────────┐
│                     Browser                          │
│                                                      │
│  Next.js 16 (App Router, React 19, TypeScript)      │
│  ┌────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ TanStack   │  │   Zustand    │  │  shadcn/ui  │  │
│  │ Query v5   │  │ Auth + Select│  │  + Tailwind │  │
│  └────────────┘  └──────────────┘  └─────────────┘  │
│         │                │                           │
│         └────────────────┘                           │
│                  │ fetch / XHR (Bearer JWT)           │
└──────────────────┼───────────────────────────────────┘
                   │ HTTP :8080
┌──────────────────┼───────────────────────────────────┐
│                  ▼   Spring Boot 4.1.1               │
│  ┌─────────────────────────────────────────────────┐ │
│  │  JwtAuthenticationFilter (OncePerRequestFilter) │ │
│  │  SecurityConfig (STATELESS, BCrypt-12, CORS)    │ │
│  └────────────────────┬────────────────────────────┘ │
│                       │                              │
│  ┌────────────────────▼────────────────────────────┐ │
│  │              REST Controllers                    │ │
│  │  Auth  │  Photo  │  PhotoAi  │  Album  │ Library│ │
│  └────────────────────┬────────────────────────────┘ │
│                       │                              │
│  ┌────────────────────▼────────────────────────────┐ │
│  │                  Services                        │ │
│  │  AuthService  │  PhotoService  │  AlbumService  │ │
│  │  AiTransformService  │  LibraryService           │ │
│  │  ImageKitService  │  JwtService  │  UserService  │ │
│  └────────────────────┬────────────────────────────┘ │
│                       │                              │
│  ┌────────────────────▼────────────────────────────┐ │
│  │           Spring Data JPA Repositories           │ │
│  │  User  │  Photo  │  Album  │  AlbumPhoto         │ │
│  │  RefreshToken                                    │ │
│  └────────────────────┬────────────────────────────┘ │
└───────────────────────┼──────────────────────────────┘
                        │ JDBC :5433
              ┌─────────▼─────────┐
              │  PostgreSQL 16    │
              │  (Docker Volume)  │
              └───────────────────┘
                        │
              ┌─────────▼────────────────┐
              │  ImageKit CDN            │
              │  ik.imagekit.io/...      │
              │  - File storage          │
              │  - AI transformations    │
              │  - On-the-fly transforms │
              └──────────────────────────┘
```

### Backend Architecture

The backend follows a strict layered architecture with clear separation of concerns.

```
backend/src/main/java/PhotoForge/backend/
│
├── config/
│   ├── AppConfig.java           # PasswordEncoder (BCrypt-12), AuthenticationManager beans
│   ├── CorsProperties.java      # Binds app.cors.* properties
│   ├── ImageKitConfig.java      # Initializes ImageKitClient singleton; fails fast if keys are missing
│   ├── ImageKitProperties.java  # Binds imagekit.* properties
│   ├── JwtProperties.java       # Binds app.jwt.* properties (secret, expiries)
│   └── SecurityConfig.java      # SecurityFilterChain: STATELESS, CSRF off, permit-list, CORS
│
├── controllers/
│   ├── AuthController.java      # /api/auth: register, login, refresh, logout, me
│   ├── PhotoController.java     # /api/photos: CRUD, upload, bulk status transitions
│   ├── PhotoAiController.java   # /api/photos/{id}/ai: preview, apply
│   ├── AlbumController.java     # /api/albums: CRUD, photo add/remove
│   └── LibraryController.java   # /api/library: storage stats, ImageKit asset browser, import
│
├── domain/                      # JPA Entities
│   ├── User.java
│   ├── Photo.java
│   ├── Album.java
│   ├── AlbumPhoto.java
│   ├── RefreshToken.java
│   ├── PhotoStatus.java         # ACTIVE | ARCHIVE | TRASH
│   └── AiTransformType.java     # 9 AI operation types
│
├── dto/                         # Java records for all API contracts
│   ├── (request records)        # LoginRequest, RegisterRequest, CreatePhotoRequest, ...
│   └── (response records)       # AuthResponse, PhotoResponse, AlbumResponse, PageResponse<T>, ...
│
├── services/
│   ├── AuthService.java         # Register/login/refresh/logout, token lifecycle
│   ├── JwtService.java          # JWT generation & validation (JJWT HS256)
│   ├── UserService.java         # User lookup helpers
│   ├── PhotoService.java        # Upload, status transitions, permanent delete
│   ├── AiTransformService.java  # Transform chain builder, preview URL, apply (poll + re-upload)
│   ├── AlbumService.java        # Album CRUD, photo membership management
│   ├── LibraryService.java      # Storage aggregation, ImageKit asset browser, import
│   └── ImageKitService.java     # All ImageKit SDK interactions: upload, delete, download, URL build
│
├── repository/                  # Spring Data JPA interfaces
│   ├── UserRepository.java
│   ├── PhotoRepository.java
│   ├── AlbumRepository.java
│   ├── AlbumPhotoRepository.java
│   └── RefreshTokenRepository.java
│
├── exceptions/                  # Custom RuntimeException subclasses
│   ├── BadRequestException.java
│   ├── ResourceNotFoundException.java
│   ├── ResourceConflictException.java
│   ├── UnauthorizedException.java
│   └── ImageKitUploadException.java
│
└── security/
    ├── JwtAuthenticationFilter.java  # OncePerRequestFilter: extracts + validates Bearer token
    └── UserDetailsServiceImpl.java   # Loads User by email for Spring Security
```

**Key design decisions:**
- **Ownership enforcement at the service layer**: every service method receives the authenticated `User` object and all repository calls always include `user.getId()` — preventing horizontal privilege escalation without any additional authorization framework.
- **All DTOs are Java records**: immutable value types annotated with Jakarta Bean Validation. Clean, zero-boilerplate API contracts.
- **Fail-fast configuration**: `ImageKitConfig` and `JwtProperties` throw `IllegalStateException` at startup if required secrets are absent, preventing runtime surprises.
- **Transactional boundaries**: service methods use `@Transactional` (or `readOnly = true` for queries), and no transaction logic leaks into controllers.

### Frontend Architecture

The frontend is a Next.js 16 App Router application using React Server Components where possible and Client Components for interactive features.

```
client/
│
├── app/
│   ├── layout.tsx                   # Root: fonts (Raleway), ThemeProvider, QueryProvider, Toaster
│   ├── page.tsx                     # Root redirect: → /photos (auth) or → /login (guest)
│   ├── globals.css
│   │
│   ├── (auth)/                      # Route group — GuestGuard wrapper
│   │   ├── layout.tsx               # Centered card layout + GuestGuard
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   │
│   └── (app)/                       # Route group — AuthGuard + AppShell wrapper
│       ├── layout.tsx               # AuthGuard + AppShell + clears selection on route change
│       ├── photos/
│       │   ├── page.tsx             # Main gallery (ACTIVE photos)
│       │   └── [id]/page.tsx        # Photo detail + AI editor
│       ├── albums/
│       │   ├── page.tsx             # Album grid
│       │   └── [id]/page.tsx        # Album photo grid
│       ├── archive/page.tsx         # Archived photos view
│       └── trash/page.tsx           # Trash view with permanent delete
│
├── components/
│   ├── auth/                        # AuthForm, AuthGuard, GuestGuard
│   ├── layout/                      # AppShell (sidebar + main), SidebarNav + StorageWidget
│   ├── photos/                      # PhotoLibraryView, PhotoGrid, PhotoBulkToolbar,
│   │                                #   PhotoAiEditor, PhotoUploadButton
│   ├── albums/                      # AlbumGrid, CreateAlbumDialog, AddToAlbumDialog
│   ├── library/                     # StorageWidget, ImportImageKitDialog
│   ├── provider/                    # QueryProvider (TanStack), ThemeProvider (next-themes)
│   └── ui/                          # ~60 shadcn/ui primitive components
│
├── hooks/
│   ├── use-auth.ts                  # useAuth, useCurrentUser, useLogin, useRegister, useLogout
│   ├── use-photos.ts                # usePhotos, useUploadPhotos, bulk mutation hooks
│   ├── use-albums.ts                # useAlbums, useAlbum, useAlbumPhotos, album mutation hooks
│   ├── use-ai.ts                    # usePhoto, useAiPreview, useAiApply, useImportImageKitAssets
│   ├── use-library.ts               # useStorageUsage, useImageKitAssets
│   └── use-mobile.ts                # Breakpoint detection for responsive sidebar
│
├── stores/
│   ├── auth-store.ts                # Zustand: accessToken, refreshToken, user — persisted to localStorage
│   └── photo-selection-store.ts     # Zustand: multi-select mode + selected photo IDs (ephemeral)
│
└── lib/
    ├── api.ts                       # Typed fetch wrapper + all API call definitions
    ├── query-keys.ts                # Hierarchical cache key factory for TanStack Query
    ├── photo-upload.ts              # XHR-based upload with real onprogress events
    ├── utils.ts                     # cn() (clsx + tailwind-merge), misc helpers
    └── validations/
        └── auth.ts                  # Zod schemas for login and register forms
```

### Data Model

```
┌──────────────────────────────────┐
│             users                │
│──────────────────────────────────│
│ id           UUID  PK            │
│ email        TEXT  UNIQUE NOT NULL│
│ passwordHash TEXT  NOT NULL      │
│ displayName  VARCHAR(100)        │
│ createdAt    TIMESTAMPTZ         │
└──────────────┬───────────────────┘
               │ 1
     ┌─────────┼──────────────────────┐
     │         │                      │
     │ *       │ *                    │ *
┌────▼─────────────────┐  ┌──────────▼──────────────────┐
│        photos        │  │          albums              │
│──────────────────────│  │──────────────────────────────│
│ id            UUID PK│  │ id            UUID PK        │
│ userId        UUID FK│  │ userId        UUID FK        │
│ imagekitFileId TEXT  │  │ title         TEXT NOT NULL  │
│ fileName      TEXT   │  │ coverPhotoId  UUID FK (Photo)│
│ url           TEXT   │  │ createdAt     TIMESTAMPTZ    │
│ thumbnailUrl  TEXT   │  │ updatedAt     TIMESTAMPTZ    │
│ mimeType      TEXT   │  └──────────────┬───────────────┘
│ sizeBytes     BIGINT │                 │ 1
│ width         INT    │                 │
│ height        INT    │                 │ *
│ status        ENUM   │  ┌──────────────▼───────────────┐
│ createdAt     TSTZ   │  │        album_photos          │
│ deletedAt     TSTZ   │◄─│──────────────────────────────│
│ parentPhotoId UUID   │  │ id         UUID PK           │
│ aiTransformType ENUM │  │ albumId    UUID FK           │
└──────────────────────┘  │ photoId    UUID FK           │
                          │ sortOrder  INT               │
                          │ addedAt    TIMESTAMPTZ       │
                          └──────────────────────────────┘

┌────────────────────────────────┐
│         refresh_tokens         │
│────────────────────────────────│
│ id        UUID  PK             │
│ userId    UUID  FK             │
│ token     VARCHAR(512) UNIQUE  │
│ expiresAt TIMESTAMPTZ          │
│ createdAt TIMESTAMPTZ          │
└────────────────────────────────┘
```

### Entity Relationships

```
User (1) ──────< Photo (many)            # user.id = photo.user_id
User (1) ──────< Album (many)            # user.id = album.user_id
User (1) ──────< RefreshToken (many)     # user.id = refresh_token.user_id
Album (1) ─────< AlbumPhoto (many)       # album.id = album_photo.album_id
Photo (1) ─────< AlbumPhoto (many)       # photo.id = album_photo.photo_id
Album >────────── Photo (optional)       # album.cover_photo_id → photo.id
Photo >────────── Photo (optional)       # photo.parent_photo_id → photo.id (AI lineage)
```

The self-referential `parentPhotoId` on `Photo` tracks the AI derivation chain: when an AI transform is applied, the resulting photo is saved as a new record with `parentPhotoId` pointing to the original and `aiTransformType` recording the operation used.

---

## REST API Reference

Base URL: `http://localhost:8080/api`

All protected endpoints require `Authorization: Bearer <access_token>`.  
All request and response bodies are JSON unless noted otherwise.

### Authentication (`/api/auth/*`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | ✗ | Create a new account |
| `POST` | `/api/auth/login` | ✗ | Authenticate and receive tokens |
| `POST` | `/api/auth/refresh` | ✗ | Exchange a refresh token for a new token pair |
| `POST` | `/api/auth/logout` | ✗ | Revoke the refresh token |
| `GET` | `/api/auth/me` | ✓ | Get the current user's profile |

**Register / Login response** (`AuthResponse`):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000.f47ac10b-58cc-4372-...",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "displayName": "Jane Doe"
  }
}
```

### Photos (`/api/photos`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/photos?status=ACTIVE&page=0&size=24` | ✓ | Paginated list by status |
| `GET` | `/api/photos/{id}` | ✓ | Single photo detail |
| `POST` | `/api/photos/upload` | ✓ | Upload a new image (`multipart/form-data`, field: `file`) |
| `POST` | `/api/photos` | ✓ | Register an already-uploaded ImageKit asset |
| `POST` | `/api/photos/archive` | ✓ | Bulk archive (`{ "photoIds": ["uuid", ...] }`) |
| `POST` | `/api/photos/trash` | ✓ | Bulk move to trash |
| `POST` | `/api/photos/restore` | ✓ | Bulk restore from archive or trash |
| `POST` | `/api/photos/delete-permanent` | ✓ | Permanently delete (must be in TRASH; deletes from ImageKit too) |
| `DELETE` | `/api/photos/{id}` | ✓ | Permanently delete single photo |

**Photo response** (`PhotoResponse`):
```json
{
  "id": "uuid",
  "imagekitFileId": "ik-file-id",
  "fileName": "sunset.jpg",
  "url": "https://ik.imagekit.io/.../sunset.jpg",
  "thumbnailUrl": "https://ik.imagekit.io/.../sunset.jpg?tr=w-400,h-400,fo-auto",
  "mimeType": "image/jpeg",
  "sizeBytes": 2048576,
  "width": 3840,
  "height": 2160,
  "status": "ACTIVE",
  "createdAt": "2025-06-01T12:00:00Z",
  "deletedAt": null,
  "parentPhotoId": null,
  "aiTransformType": null
}
```

**Paginated response** (`PageResponse<T>`):
```json
{
  "content": [ ...photos ],
  "page": 0,
  "size": 24,
  "totalElements": 157,
  "totalPages": 7,
  "last": false
}
```

### AI Transforms (`/api/photos/{id}/ai`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/photos/{id}/ai/preview` | ✓ | Build and return a preview URL (no upload) |
| `POST` | `/api/photos/{id}/ai/apply` | ✓ | Apply transformation and save as new photo |

**Request body** (`AiTransformRequest`):
```json
{
  "type": "GENERATIVE_FILL",
  "prompt": "serene mountain landscape",
  "width": 1920,
  "height": 1080
}
```

**Preview response** (`AiTransformPreviewResponse`):
```json
{
  "previewUrl": "https://ik.imagekit.io/.../photo.jpg?tr=bg-genfill-prompt-...,w-1920,h-1080,cm-pad_resize",
  "type": "GENERATIVE_FILL",
  "transformChain": "bg-genfill-prompt-serene%20mountain%20landscape,w-1920,h-1080,cm-pad_resize"
}
```

### Albums (`/api/albums`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/albums` | ✓ | All albums for current user (sorted by `updatedAt DESC`) |
| `POST` | `/api/albums` | ✓ | Create album (`{ "title": "Summer 2025" }`) |
| `GET` | `/api/albums/{id}` | ✓ | Album detail with cover and photo count |
| `PATCH` | `/api/albums/{id}` | ✓ | Update title or cover photo |
| `DELETE` | `/api/albums/{id}` | ✓ | Delete album (photos are unaffected) |
| `GET` | `/api/albums/{id}/photos?page=0&size=24` | ✓ | Paginated photos in album |
| `POST` | `/api/albums/{id}/photos` | ✓ | Add photos (`{ "photoIds": [...] }`) |
| `DELETE` | `/api/albums/{id}/photos/{photoId}` | ✓ | Remove one photo from album |

### Library (`/api/library`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/library/storage` | ✓ | Storage usage (bytes used + photo count) |
| `GET` | `/api/library/imagekit-assets` | ✓ | All files in user's ImageKit folder with `alreadyImported` flag |
| `POST` | `/api/library/import` | ✓ | Import selected ImageKit assets into PhotoForge |

---

## Security Design

### JWT Strategy

PhotoForge uses a **dual-token stateless auth** strategy:

```
Client                          Server
  │                                │
  │──── POST /auth/login ─────────►│
  │                                │  1. Authenticate via Spring AuthenticationManager
  │                                │  2. Delete existing refresh tokens for user
  │                                │  3. Generate HS256 JWT (15-min TTL)
  │                                │  4. Generate opaque refresh token (two UUID v4s joined by ".")
  │                                │  5. Persist refresh token to DB
  │◄─── AuthResponse (both tokens)─│
  │                                │
  │── GET /photos (Bearer JWT) ───►│
  │                                │  JwtAuthenticationFilter:
  │                                │    a. Extract Bearer token
  │                                │    b. Validate HMAC-SHA256 signature
  │                                │    c. Parse subject (userId UUID)
  │                                │    d. Load User from DB
  │                                │    e. Set SecurityContext
  │◄── 200 OK ─────────────────────│
```

- **Access token**: JJWT-signed HS256 JWT, 15-minute TTL. Claims: `sub` (userId), `email`, `type: "access"`.
- **Refresh token**: opaque random string (`UUID.randomUUID() + "." + UUID.randomUUID()`), stored as a DB row with expiry timestamp. Expires in 7 days.
- **Password hashing**: BCrypt with cost factor 12.

### Refresh Token Rotation

Every token refresh is a full rotation — the old token is immediately deleted and a brand-new pair is issued. This limits the impact of token theft: a stolen refresh token can only be used once before it is invalidated.

```
Client                             Server
  │                                   │
  │── POST /auth/refresh ────────────►│
  │   { refreshToken: "uuid.uuid" }   │  1. Find RefreshToken row by token value
  │                                   │  2. Check expiry (delete + 401 if expired)
  │                                   │  3. Delete the old token (rotation)
  │                                   │  4. Issue new access JWT + new refresh token
  │                                   │  5. Persist new refresh token to DB
  │◄── AuthResponse (new pair) ───────│
```

---

## AI Transformation Engine

### Supported Operations

| Operation | `type` | Required Fields | ImageKit Transform |
|---|---|---|---|
| Background Removal | `REMOVE_BACKGROUND` | — | `e-bgremove` |
| Background Removal + Drop Shadow | `BACKGROUND_AND_SHADOW` | — | `e-bgremove:e-dropshadow` |
| Generative Background Change | `CHANGE_BACKGROUND` | `prompt` | `e-changebg-prompt-{prompt}` |
| Generative Fill | `GENERATIVE_FILL` | `width`, `height` | `bg-genfill[-prompt-{prompt}],w-{w},h-{h},cm-pad_resize` |
| Smart Crop | `SMART_CROP` | `width`, `height` | `w-{w},h-{h},fo-auto` |
| Object-Aware Crop | `OBJECT_CROP` | `focusObject` | `fo-{sanitized object}` |
| Portrait Retouch | `RETOUCH` | — | `e-retouch` |
| AI Upscale | `UPSCALE` | — | `e-upscale` |
| Free-form AI Edit | `AI_EDIT` | `prompt` | `e-edit-prompt-{prompt}` |

Dimension constraints for operations requiring `width` / `height`: 64–4096 px. The `focusObject` field is sanitized to `[a-z0-9_-]` to prevent injection into the ImageKit URL.

### Preview vs Apply Flow

**Preview** is instant and costs no storage:
1. Client POSTs `AiTransformRequest` to `/api/photos/{id}/ai/preview`
2. `AiTransformService` builds the ImageKit transform chain string
3. Returns a URL with `?tr={chain}` appended — ImageKit renders it on-demand at view time
4. Nothing is uploaded or saved; the frontend renders the URL directly

**Apply** saves the result as a new, permanent photo:
1. Client POSTs to `/api/photos/{id}/ai/apply`
2. Same transform chain is built
3. `ImageKitService.downloadTransformedImage()` polls the transform URL with **up to 12 retries every 3 seconds** (36-second max), watching for the `is-intermediate-response: true` header that ImageKit sets while processing. HTML responses (ImageKit's processing page) are also retried.
4. The resolved image bytes are re-uploaded to ImageKit under `/users/{userId}/photo-ai-{operation}.ext`
5. A new `Photo` entity is persisted with:
   - `parentPhotoId` → original photo's UUID
   - `aiTransformType` → the applied operation
   - `status` → `ACTIVE`
6. The new `PhotoResponse` is returned and the UI navigates to the new photo's detail page

---

## Photo Lifecycle

Photos move through three states. Transitions are enforced at the service layer.

```
               upload / import
                     │
                     ▼
              ┌─────────────┐
              │   ACTIVE    │◄────────────────────┐
              └──────┬──────┘                     │
                     │                          restore
             archive │                             │
                     ▼                             │
              ┌─────────────┐                      │
              │   ARCHIVE   │                      │
              └──────┬──────┘                      │
                     │                             │
              trash  │◄────── (also from ACTIVE)───┘
                     ▼
              ┌─────────────┐
              │    TRASH    │──── permanent delete ──► DB row + ImageKit file deleted
              └─────────────┘      (only from TRASH)
```

- `ARCHIVE` → keeps the photo out of the main library view without risking deletion
- `TRASH` → sets `deletedAt` timestamp; soft-deleted from the user's perspective
- **Permanent delete** calls `imageKitService.deleteFile(imagekitFileId)` before removing the DB row — no orphaned CDN files
- Bulk operations use a single `findByIdInAndUserId` query and validate that every requested ID was found before applying any mutation (all-or-nothing semantics)

---

## ImageKit CDN Integration

ImageKit serves as both the file storage layer and the AI processing engine.

**File organization**: all files are stored under a per-user path `/users/{userId}` with `useUniqueFileName = true` to prevent naming collisions.

**Upload flow** (new photo):
1. Multipart file arrives at the Spring backend
2. `ImageKitService.uploadPhoto()` streams the bytes to ImageKit via the Java SDK
3. ImageKit returns metadata: `fileId`, `url`, `thumbnailUrl`, `filePath`, `width`, `height`, `size`
4. If ImageKit doesn't return a `thumbnailUrl`, the app builds one: `{base_url}?tr=w-400,h-400,fo-auto`
5. All metadata is persisted as a `Photo` entity

**Thumbnail strategy**: lazy, CDN-side. Thumbnails are ImageKit transform URLs that ImageKit generates and caches on first request. No separate thumbnail upload.

**AI transform re-upload flow**: after polling for a completed transform, `ImageKitService.uploadBytes()` re-uploads the raw bytes with a derived filename (`original-ai-remove-background.jpg`) into the same user folder.

**Next.js image optimization**: `next.config.ts` whitelists `ik.imagekit.io` as a remote pattern, enabling Next.js `<Image>` component optimization for all CDN-served photos.

---

## Frontend Deep Dive

### Routing (Next.js App Router)

Route groups provide layout isolation without URL segments:

- `(auth)` — wraps login and register in a `GuestGuard` that redirects authenticated users to `/photos`
- `(app)` — wraps all protected views in an `AuthGuard` (redirects unauthenticated users to `/login`) and mounts the `AppShell` (sidebar + main content area)

The `(app)` layout also runs a `useEffect` on `pathname` changes to automatically clear the multi-select state (`usePhotoSelectionStore`) when navigating between views.

### State Management

**TanStack React Query** manages all server state:
- Queries are gated on `!!accessToken` — no API calls fire before auth is confirmed
- Hierarchical query keys (`["photos", "list", "ACTIVE"]`) enable surgical cache invalidation: archiving a photo invalidates `["photos"]` to refresh all photo-related views at once
- Photo and album-photo lists use `useInfiniteQuery` — the server's `page` number is used as the cursor; the "Load More" button calls `fetchNextPage()`

**Zustand** manages two slices of client state:
- `useAuthStore` (persisted via `localStorage`, key: `gp-auth`): stores `accessToken`, `refreshToken`, `user`. An `isReady` flag prevents the UI from rendering in an unauthenticated state while the persisted store is still rehydrating from `localStorage`.
- `usePhotoSelectionStore` (ephemeral): tracks whether multi-select mode is active and the `Set<string>` of selected photo IDs. The `PhotoBulkToolbar` reads this store to show context-appropriate bulk actions (different actions for ACTIVE vs ARCHIVE vs TRASH views).

### Component Architecture

**`PhotoLibraryView`** is a shared, reusable view that powers the main gallery (`/photos`), archive (`/archive`), and trash (`/trash`) pages. It accepts a `status` prop and adapts its toolbar actions accordingly — no code duplication for the three status views.

**`PhotoAiEditor`** renders the AI transform panel on the photo detail page. It manages:
- Operation selection (9 options with optional parameter fields)
- Live preview rendering (the preview URL is loaded directly into an `<Image>` component)
- Apply trigger with loading state
- Navigation to the new photo on successful apply

**`PhotoUploadButton`** uses `XMLHttpRequest` rather than `fetch` to get real `onprogress` events for per-file upload progress bars. The native `fetch` API does not support upload progress tracking.

**`AppShell`** renders a fixed sidebar on desktop and a slide-out drawer on mobile, with the breakpoint detected via `useMobile()`. The sidebar includes the `StorageWidget` (a progress bar showing `usedBytes / 5 GB`).

### API Layer

`lib/api.ts` is the single source of truth for all API communication. It exports:
- All TypeScript types mirroring the backend's Java records
- A typed `request<T>()` wrapper around `fetch` that injects the `Authorization` header from the Zustand store on every call
- The `api` object with one method per endpoint

`lib/photo-upload.ts` is the only place that bypasses `request()` — it uses raw `XMLHttpRequest` directly to support upload progress events.

---

## Project Structure

```
PhotoForge/
│
├── backend/                     # Spring Boot 4.1.1 application
│   ├── pom.xml
│   └── src/main/java/PhotoForge/backend/
│       ├── config/
│       ├── controllers/
│       ├── domain/
│       ├── dto/
│       ├── exceptions/
│       ├── repository/
│       ├── security/
│       └── services/
│
├── client/                      # Next.js 16 application
│   ├── app/
│   ├── components/
│   ├── hooks/
│   ├── lib/
│   ├── stores/
│   └── package.json
│
├── docker-compose.yml           # PostgreSQL 16 on port 5433
├── .env                         # Root secrets (loaded by Spring Boot)
└── README.md
```

---

## Getting Started

### Prerequisites

- **Java 17+** (`java --version`)
- **Maven 3.9+** (`mvn --version`)
- **Node.js 20+** (`node --version`)
- **Docker + Docker Compose** (for PostgreSQL)
- An **ImageKit account** with a public key, private key, and URL endpoint

### Environment Setup

Create a `.env` file at the project root (the `backend/` directory reads from `../\.env` via `spring.config.import`):

```env
JWT_SECRET=your_minimum_32_character_secret_here
IMAGEKIT_PRIVATE_KEY=private_...
IMAGEKIT_PUBLIC_KEY=public_...
IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/your_id
```

For the frontend, create `client/.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### Running the App

**1. Start the database**
```bash
docker-compose up -d
```
This starts a PostgreSQL 16 container on port `5433` with a persistent named volume `postgres_data`.

**2. Start the backend**
```bash
cd backend
./mvnw spring-boot:run
```
The API starts on `http://localhost:8080`. Hibernate auto-updates the schema on first run.

**3. Start the frontend**
```bash
cd client
npm install
npm run dev
```
The app is available at `http://localhost:3000`.

---

## Configuration Reference

All backend configuration lives in `backend/src/main/resources/application.properties`.

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/PhotoForge` | PostgreSQL JDBC URL |
| `spring.datasource.username` | `postgres` | DB username |
| `spring.datasource.password` | `postgres` | DB password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management (use `validate` in production) |
| `app.cors.allowed-origins` | `http://localhost:3000` | Comma-separated CORS origins |
| `app.jwt.secret` | from `JWT_SECRET` env var | HMAC secret for signing JWTs (min 32 chars) |
| `app.jwt.access-expiration-ms` | `900000` (15 min) | Access token TTL in milliseconds |
| `app.jwt.refresh-expiration-ms` | `604800000` (7 days) | Refresh token TTL in milliseconds |
| `imagekit.public-key` | from `IMAGEKIT_PUBLIC_KEY` | ImageKit public key |
| `imagekit.private-key` | from `IMAGEKIT_PRIVATE_KEY` | ImageKit private key |
| `imagekit.url-endpoint` | from `IMAGEKIT_URL_ENDPOINT` | ImageKit CDN base URL |
| `spring.servlet.multipart.max-file-size` | `20MB` | Max single file upload size |
| `spring.servlet.multipart.max-request-size` | `25MB` | Max total request size |
