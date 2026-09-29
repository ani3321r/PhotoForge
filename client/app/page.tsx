"use client"
import { Button } from "@base-ui/react";
import { useAuth, useLogout } from "@/hooks/use-auth";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Spinner } from "@/components/ui/spinner";

export default function HomePage() {
  const router = useRouter();
  const {isReady, isLoggedIn} =  useAuth();

  useEffect(() =>{
    if(!isReady) return;
    router.replace(isLoggedIn ? "/photos": "/login");
  }, [isReady, isLoggedIn, router])
  return (
    <div className="flex min-h-full items-center justify-center bg-background text-muted-foreground">
      <Spinner className="size-6"/>
    </div>
  );
}
