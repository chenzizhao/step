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
 * Adds a random greeting to the page.
 */

function loadPage() {
  getComments();
  addLoginStatus();
}

function findZoe() {
  const locationToLink = {
    '43.5˚N, 79.4˚W':
      `https://www.google.com/maps/place/43%C2%B030'00.0%22N+79%C2%B024'00.0%22W/`,
    'a very very dark place': 'https://ssh.cloud.google.com/cloudshell',
    'github': 'https://github.com/chenzizhao',
    'linkedin': 'https://www.linkedin.com/in/chenzizhao/',
    'the pit': 'https://skulepedia.ca/wiki/Sandford_Fleming_Atrium',
  };

  // Pick a random location.
  const locations = Object.keys(locationToLink);
  const location = locations[Math.floor(Math.random() * locations.length)];

  // Add it to the page.
  const link = document.getElementById('link');
  link.innerText = location;
  link.href = locationToLink[location];
}

function grow() {
  const srcToAlt = {
    'images/basil1.jpg': 'Basil Day 0',
    'images/basil2.jpg': 'Basil Day 10',
    'images/basil3.jpg': 'Basil Day 20',
    'images/basil4.jpg': 'Basil Day 30',
    'images/basil5.jpg': 'Basil Day 40',
    'images/basil6.jpg': 'Basil Day 60',
  };
  const image = document.getElementById('basil');
  const endMsg = document.getElementById('endMsg');
  console.log(`Let's grow some basil`);
  // Clear text from the previous round of slideshow.
  endMsg.innerText = '';

  for (let imgIndex = 1; imgIndex <= 6; imgIndex++) {
    setTimeout(() => updateImg(imgIndex), 1000 * imgIndex)
  }

  function updateImg(imgIndex) {
    const imgSrc = `images/basil${imgIndex}.jpg`;
    image.src = imgSrc;
    image.alt = srcToAlt[imgSrc];
    if (imgIndex >= Object.keys(srcToAlt).length) {
      endMsg.innerText = `That's all the photos I have for now!`;
    }
  }
}

function getComments() {
  const limit = document.getElementById('limit').value;

  fetch(`/data?limit=${limit}`)
    .then(response => response.json())
    .then(comments => {
      const commentsContainer = document.getElementById('comments-container');
      commentsContainer.innerHTML = '';

      for (const comment of comments) {
        const commentElement = document.createElement('div');
        commentElement.innerText = `${comment.content} -- ${comment.likeCount}`;
        commentElement.className = 'comment-container';
        const likeButton = createButton('👍', function () { likeComment(comment.id); })
        commentElement.appendChild(likeButton);
        commentsContainer.appendChild(commentElement);
      }
    });
}

function deleteComments() {
  const request = new Request('/data', { method: 'DELETE' });
  fetch(request).then(() => getComments());
}

function submitComment() {
  const newComment = document.getElementById('new-comment').value;
  const request = new Request(`/data?newComment=${newComment}`, { method: 'POST' });
  fetch(request).then(() => getComments());
}

function likeComment(commentId) {
  const request = new Request(`/like?commentId=${commentId}`, { method: 'POST' })
  fetch(request).then(() => getComments());
}

function addLoginStatus() {
  const request = new Request('/login', { method: 'GET' });
  fetch(request)
    .then(response => response.json())
    .then(userLoginData => {
      const deleteButtonContainer = document.getElementById('delete-container');
      const commentSubmissionForm = document.getElementById('comment-submission-form');
      const loginContainer = document.getElementById('login-container');
      if (userLoginData.isLoggedIn) {
        deleteButtonContainer.appendChild(createButton('Delete', deleteComments));
        commentSubmissionForm.appendChild(createInputForm('Leave a comment: ', 'new-comment'));
        commentSubmissionForm.appendChild(createButton('Submit', submitComment));
        loginContainer.innerHTML = '';
        loginContainer.appendChild(createLink('Log Out', userLoginData.url));
      }
      else {
        deleteButtonContainer.innerHTML = '';
        commentSubmissionForm.innerHTML = '';
        loginContainer.innerHTML = '';
        loginContainer.appendChild(createLink('Log In with Google Account', userLoginData.url));
      }
    })
}

function createButton(msg, onclickFunc) {
  const button = document.createElement('button');
  button.innerText = msg;
  button.onclick = onclickFunc;
  return button;
}

function createInputForm(msg, id) {
  const inputForm = document.createElement('input');
  inputForm.placeholder = msg;
  inputForm.id = id;
  return inputForm;
}

function createLink(msg, url) {
  const link = document.createElement('a');
  link.innerText = msg;
  link.href = url;
  return link;
}