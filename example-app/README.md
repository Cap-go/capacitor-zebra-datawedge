# Example App for `@capgo/capacitor-zebra-datawedge`

This Vite project links directly to the local plugin source so you can validate DataWedge status queries and soft scans on a Zebra Android device.

## Getting started

```bash
bun install
bun run start
```

To test on native shells:

```bash
bunx cap add android
bunx cap sync
```

Before triggering scans, configure a Zebra DataWedge profile with:

- `Intent Output` enabled
- `Broadcast Intent` delivery
- an intent action matching the value in the example input field
