// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * Redirects the user back to the home page when the cancel button is clicked.
 */
function cancelUpload() {
  window.location.href = 'index.html';
}

/**
 * Sends a request to add a receipt to Blobstore.
 */
async function uploadReceipt(event) {
  // Prevent the default action of reloading the page on form submission.
  event.preventDefault();

  const fileInput = document.getElementById('receipt-image-input');
  if (fileInput.files.length === 0) {
    alert('A JPEG image is required.');
    return;
  }

  const uploadUrl = await fetchBlobstoreUrl();
  const label = document.getElementById('label-input').value;
  const image = fileInput.files[0];

  const formData = new FormData();
  formData.append('label', label);
  formData.append('receipt-image', image);

  const response = await fetch(uploadUrl, {method: 'POST', body: formData});

  // Create an alert if there is an error.
  if (response.status !== 200) {
    alert(await response.text());
    return;
  }

  // TODO: Redirect to receipt analysis page for MVP
  window.location.href = '/';
}

/**
 * Gets a Blobstore upload URL for uploading a receipt image.
 * @return {string} A Blobstore upload URL.
 */
async function fetchBlobstoreUrl() {
  const response = await fetch('/upload-receipt');
  const imageUploadUrl = await response.text();
  return imageUploadUrl;
}

/**
 * Converts the formatted price back to a number when the user
 * selects the price input.
 */
function convertPricetoValue(event) {
  const value = event.target.value;
  event.target.value = value ? convertStringToNumber(value) : '';
}

/**
 * Converts a string value into a number, removing all non-numeric characters.
 */
function convertStringToNumber(string) {
  return Number(String(string).replace(/[^0-9.]+/g, ''));
}

/**
 * Converts the number inputted by the user to a formatted string when
 * the user unfocuses from the price input.
 */
function formatCurrency(event) {
  const value = event.target.value;

  if (value) {
    event.target.value =
        convertStringToNumber(value).toLocaleString(undefined, {
          maximumFractionDigits: 2,
          currency: 'USD',
          style: 'currency',
          currencyDisplay: 'symbol',
        });
  } else {
    event.target.value = '';
  }
}

/**
 * Adds the selected file name to the input label and checks that the size of
 * the uploaded file is within the limit.
 */
function displayFileName() {
  const fileLabel = document.getElementById('receipt-filename-label');
  const DEFAULT_FILE_LABEL = 'Choose file';

  if (checkFileSize()) {
    const fileInput = document.getElementById('receipt-image-input');
    const fileName = fileInput.value.split('\\').pop();
    fileLabel.innerText = fileName;
  } else {
    fileLabel.innerText = DEFAULT_FILE_LABEL;
  }
}

/**
 * Displays an error message if the user selects a file larger than 5 MB.
 * @return {boolean} Whether a file is selected and has size less than 5 MB.
 */
function checkFileSize() {
  const fileInput = document.getElementById('receipt-image-input');
  const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

  // Return if the user did not select a file.
  if (fileInput.files.length === 0) {
    return false;
  }

  if (fileInput.files[0].size > MAX_FILE_SIZE_BYTES) {
    alert('The selected file exceeds the maximum file size of 5 MB.');
    fileInput.value = '';
    return false;
  }

  return true;
}
