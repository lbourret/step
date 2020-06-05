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
function addRandomGreeting() {
  const facts =
      ['My favorite color is purple',  'I love french fries', 'I play soccer', 'My favorite ice cream flavor is coffee',
      'Connecticut born and raised', 'I have never watched Game of Thrones'];

  // Pick a random greeting.
  const fact = facts[Math.floor(Math.random() * facts.length)];

  // Add it to the page.
  const factContainer = document.getElementById('fact-container');
  factContainer.innerText = fact;
}

/**
 * Toggles visibility class
 */
function toggleText(divID) {
    var content = document.getElementById(divID);
    content.classList.toggle('visible');
}

/**
 * Retrieves comments from server
 */
function getComment() {
  var limit = document.getElementById('limit').value;
  fetch('/list-comments?limit='+limit).then(response => response.json()).then((comments) => {

    const commentListElement = document.getElementById('comment-container');
    commentListElement.innerHTML = 'Comments: ';
    comments.forEach((comment) => {
        commentListElement.appendChild(createCommentElement(comment));
    })    
  });
}

/** Creates an <li> element containing text. */
function createCommentElement(comment) {
  const commentElement = document.createElement('li');
  commentElement.innerText = comment.title;

  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteComment(comment);

  // Remove the task from the DOM.
    commentElement.remove();
  });
  commentElement.appendChild(deleteButtonElement);
  return commentElement;
}

/** Set limit's value to limit from URL */
function initParam(){
  const limit = getURLParam('limit');
  document.getElementById('limit').value = limit;
}

/** Get limit parameter from URL */
function getURLParam(paramName){
  const params = new URLSearchParams(window.location.search);
  return params.get(paramName);
}

/** 
    Tells the server to delete the comment.
    @param comment if entered, specific comment will delete else all comments will delete
*/
async function deleteComment(comment) {
 if (!comment) {
  const response = await fetch('/delete-all-comments', {method: 'POST'});
  const comments = await response.text();
  document.getElementById('comment-container').innerText = 'COMMENTS:' + comments;
 } else {
  const params = new URLSearchParams();
  params.append('id', comment.id);
  fetch('/delete-comment', {method: 'POST', body: params});
 }
}