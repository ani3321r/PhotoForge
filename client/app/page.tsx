"use client"
import { Button } from "@base-ui/react";
import Image from "next/image";
import { useLogout } from "@/hooks/use-auth";

export default function Home() {
  const logoutMutation = useLogout();
  return (
    <div className="flex flex-col flex-1 items-center justify-center bg-zinc-50 font-sans dark:bg-black">
      <Button onClick={() => logoutMutation.mutate()}>Logout</Button>
    </div>
  );
}
