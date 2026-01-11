<template>
  <svg
    v-if="iconType === 'folder'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path
      d="M880 298.4H521L403.7 186.2c-1.5-1.4-3.5-2.2-5.5-2.2H144c-17.7 0-32 14.3-32 32v592c0 17.7 14.3 32 32 32h736c17.7 0 32-14.3 32-32V330.4c0-17.7-14.3-32-32-32z"
      fill="#409EFF"
    />
    <path
      d="M880 298.4H521L403.7 186.2c-1.5-1.4-3.5-2.2-5.5-2.2H144c-17.7 0-32 14.3-32 32v592c0 17.7 14.3 32 32 32h736c17.7 0 32-14.3 32-32V330.4c0-17.7-14.3-32-32-32z"
      fill-opacity=".1"
      fill="#409EFF"
    />
  </svg>

  <svg
    v-else-if="iconType === 'image'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M160 160h704v704H160z" fill="#E8F3FF" />
    <path
      d="M800 216v592H224V216h576m32-56H192c-17.7 0-32 14.3-32 32v640c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V192c0-17.7-14.3-32-32-32z"
      fill="#3A8EE6"
    />
    <path
      d="M328 424a48 48 0 1 0 96 0 48 48 0 1 0-96 0zM728 664H296l136-224 96 152 72-104z"
      fill="#67C23A"
    />
  </svg>

  <svg
    v-else-if="iconType === 'video'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M160 160h704v704H160z" fill="#FFF3E8" />
    <path
      d="M800 216v592H224V216h576m32-56H192c-17.7 0-32 14.3-32 32v640c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V192c0-17.7-14.3-32-32-32z"
      fill="#FA8C16"
    />
    <path
      d="M440 376v272a8 8 0 0 0 12.8 6.4l192-136a8 8 0 0 0 0-12.8l-192-136A8 8 0 0 0 440 376z"
      fill="#FA8C16"
    />
  </svg>

  <svg
    v-else-if="iconType === 'audio'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M160 160h704v704H160z" fill="#FFF0F0" />
    <path
      d="M800 216v592H224V216h576m32-56H192c-17.7 0-32 14.3-32 32v640c0 17.7 14.3 32 32 32h640c17.7 0 32-14.3 32-32V192c0-17.7-14.3-32-32-32z"
      fill="#F5222D"
    />
    <path
      d="M512 360c-61.9 0-112 50.1-112 112v80c0 61.9 50.1 112 112 112s112-50.1 112-112v-80c0-61.9-50.1-112-112-112z"
      fill="#F5222D"
    />
  </svg>

  <svg
    v-else-if="iconType === 'pdf'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M232 144h560v736H232z" fill="#FFF0F0" />
    <path d="M232 280h560v120H232z" fill="#F5222D" />
    <text x="320" y="370" font-size="80" fill="#fff" font-weight="bold">PDF</text>
    <path d="M232 144h560v736H232z" fill="none" stroke="#F5222D" stroke-width="32" />
  </svg>

  <svg
    v-else-if="iconType === 'word'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M232 144h560v736H232z" fill="#E6F7FF" />
    <path d="M232 280h560v120H232z" fill="#1890FF" />
    <text x="290" y="370" font-size="80" fill="#fff" font-weight="bold">WORD</text>
    <path d="M232 144h560v736H232z" fill="none" stroke="#1890FF" stroke-width="32" />
  </svg>

  <svg
    v-else-if="iconType === 'excel'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M232 144h560v736H232z" fill="#F6FFED" />
    <path d="M232 280h560v120H232z" fill="#52C41A" />
    <text x="280" y="370" font-size="80" fill="#fff" font-weight="bold">EXCEL</text>
    <path d="M232 144h560v736H232z" fill="none" stroke="#52C41A" stroke-width="32" />
  </svg>

  <svg
    v-else-if="iconType === 'ppt'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M232 144h560v736H232z" fill="#FFF7E6" />
    <path d="M232 280h560v120H232z" fill="#FA8C16" />
    <text x="320" y="370" font-size="80" fill="#fff" font-weight="bold">PPT</text>
    <path d="M232 144h560v736H232z" fill="none" stroke="#FA8C16" stroke-width="32" />
  </svg>

  <svg
    v-else-if="iconType === 'zip'"
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M232 144h560v736H232z" fill="#F9F0FF" />
    <path d="M232 280h560v120H232z" fill="#722ED1" />
    <text x="330" y="370" font-size="80" fill="#fff" font-weight="bold">ZIP</text>
    <path d="M232 144h560v736H232z" fill="none" stroke="#722ED1" stroke-width="32" />
  </svg>

  <svg
    v-else
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 1024 1024"
    :width="size"
    :height="size"
  >
    <path d="M232 144h560v736H232z" fill="#F5F5F5" />
    <path d="M664 144v224h128" fill="#D9D9D9" />
    <path d="M232 144h560v736H232z" fill="none" stroke="#8C8C8C" stroke-width="32" />
  </svg>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  icon: {
    type: String,
    default: 'file'
  },
  fileType: {
    type: String,
    default: ''
  },
  isFolder: {
    type: Number,
    default: 0
  },
  size: {
    type: [Number, String],
    default: 32
  }
});

const iconType = computed(() => {
  if (props.isFolder === 1) return 'folder';
  
  if (props.icon) {
    const icon = props.icon.toLowerCase();
    if (['image', 'video', 'audio', 'pdf', 'word', 'excel', 'ppt', 'zip'].includes(icon)) {
      return icon;
    }
  }

  const type = props.fileType.toLowerCase();
  if (type.startsWith('image/')) return 'image';
  if (type.startsWith('video/')) return 'video';
  if (type.startsWith('audio/')) return 'audio';
  if (type.includes('pdf')) return 'pdf';
  if (type.includes('word') || type.includes('document')) return 'word';
  if (type.includes('excel') || type.includes('sheet')) return 'excel';
  if (type.includes('powerpoint') || type.includes('presentation')) return 'ppt';
  if (type.includes('zip') || type.includes('compressed') || type.includes('tar') || type.includes('rar')) return 'zip';
  
  return 'file';
});
</script>