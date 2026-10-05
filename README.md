**Create: Curve Casing Fix** fixes the casing used by flat curved tracks from [Create: Steam 'n' Rails](https://modrinth.com/mod/create-steam-n-rails)

Flat curved tracks normally use thin 3px panels for their casing, which makes them visually inconsistent with straight cased tracks. This mod changes the casing of flat curved tracks to use the same slab-style casing as straight tracks, including the corresponding rail grooves.

### Known Issues
The current implementation has some visual limitations on sharp curves:
* **Gaps between segments** may be visible on sharp curves, where adjacent casing segments do not meet perfectly
* **Texture stretching** can occur at the ends of curved segments, causing the casing texture to appear distorted

These are purely visual issues and are related to how the curved track geometry is generated.

> **⚠️ Important**  
> This is a **client-side visual fix** and does **not** add new blocks, items, or gameplay mechanics.

**Requires:**
* [Create](https://modrinth.com/mod/create)
* [Create: Steam 'n' Rails](https://modrinth.com/mod/create-steam-n-rails)
